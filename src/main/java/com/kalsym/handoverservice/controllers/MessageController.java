package com.kalsym.handoverservice.controllers;

import com.kalsym.handoverservice.VersionHolder;
import com.kalsym.handoverservice.agent.models.VisitorPayload;
import com.kalsym.handoverservice.models.*;
import com.kalsym.handoverservice.agent.models.*;
import com.kalsym.handoverservice.enums.MediaType;
import com.kalsym.handoverservice.enums.MessageType;
import com.kalsym.handoverservice.services.AgentInterfaceService;
import com.kalsym.handoverservice.services.ChannelInterfaceService;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author z33Sh
 */
@RestController
@RequestMapping("/")
public class MessageController {

    private static final Logger LOG = LoggerFactory.getLogger("application");
    @Autowired
    private Environment env;

    @Autowired
    private AgentInterfaceService agentInterfaceService;
    @Autowired
    private ChannelInterfaceService channelInterfaceService;

    /**
     * Endpoint for receiving customer messages from different channel wrappers.
     * Validates and forward the incoming message to live agents interface.
     *
     * @param request
     * @param senderId
     * @param refrenceId
     * @param requestBody
     * @return
     */
    @PostMapping(path = {"inbound/customer/message"}, name = "inbound-customer-message-post")
    public ResponseEntity<HttpReponse> inbound(HttpServletRequest request,
            @RequestParam(name = "senderId", required = true) String senderId,
            @RequestParam(name = "refrenceId", required = true) String refrenceId,
            @RequestBody(required = true) RequestPayload requestBody) {
        try {
            LOG.info("[v{}] Request received from [{}]  with {}", VersionHolder.VERSION, senderId, "queryString: " + request.getQueryString());
            if (null != requestBody) {
                LOG.info("[v{}][{}] {}", VersionHolder.VERSION, senderId, "body: " + requestBody.toString());

                // TODO: 1 - Register/update Visitor
                List<CustomField> customFields = new ArrayList<>();
                customFields.add(new CustomField(CustomFields.callbackUrl, requestBody.getCallbackUrl(), true));
                customFields.add(new CustomField(CustomFields.data, requestBody.getData(), true));
                customFields.add(new CustomField(CustomFields.isGuest, requestBody.getIsGuest() + "", true));
                customFields.add(new CustomField(CustomFields.msgId, requestBody.getMsgId(), true));
                customFields.add(new CustomField(CustomFields.referral, requestBody.getReferral(), true));
                String roomId = refrenceId + "r";
                String token = senderId;

                Visitor visitor = new Visitor(senderId, token, "", customFields);
                VisitorPayload visitorPayload = new VisitorPayload(visitor);
                JSONObject visitorRegistrationResponse = agentInterfaceService.registerOrUpdateVisitor(visitorPayload, token);
                boolean isVisitorRegistrationSuccess = visitorRegistrationResponse.getBoolean("success");
                LOG.info("[{}] [{}] is visitor registration Success:  [{}] ", VersionHolder.VERSION, senderId, isVisitorRegistrationSuccess);
                if (isVisitorRegistrationSuccess) {

                    // TODO: 2 - Create/Update Room
                    JSONObject roomCreationResponse = agentInterfaceService.createOrUpdateRoom(token, roomId, refrenceId);
                    boolean isRoomCreationSuccesss = roomCreationResponse.getBoolean("success");
                    LOG.info("[{}] [{}] is room creation Success:  [{}] ", VersionHolder.VERSION, senderId, isVisitorRegistrationSuccess);
                    if (isRoomCreationSuccesss) {
                        // TODO: 3 - Send customer message to agent interface
                        Message msg = new Message(token, roomId, requestBody.getData());
                        JSONObject sendMessageResponse = agentInterfaceService.sendMessage(msg, refrenceId);
                        boolean isSendMessageSuccesss = sendMessageResponse.getBoolean("success");
                        LOG.info("[{}] [{}] is message sent Successfully:  [{}] ", VersionHolder.VERSION, senderId, isSendMessageSuccesss);

                    }
                }
            } else {
                LOG.warn("null or empty requestBody: {}", requestBody);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
        } catch (Exception ex) {
            LOG.warn("Processing of inbound message failed: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /**
     * Endpoint for receiving messages from agents interface. Validates and
     * forward the outbound message to respective wrapper interface.
     *
     * @param requestData
     * @return
     */
    @RequestMapping(value = "outbound/agent/message", method = RequestMethod.POST, consumes = "Application/json")
    public ResponseEntity<Void> takeConversationFromCustomerService(@RequestBody String requestData) {
        try {
            LOG.info("requestData: [{}]", requestData);

            JSONObject requestObject = new JSONObject(requestData);

            LOG.info("requestObject: [{}]", requestObject);

            if (requestObject.has("type") && "Message".equalsIgnoreCase(requestObject.getString("type")) && requestObject.has("messages") && requestObject.has("visitor")) {
                JSONObject visitorData = requestObject.getJSONObject("visitor");
                JSONObject customFields = visitorData.getJSONObject("customFields");
                String agentName = requestObject.getJSONObject("agent").getString("username");
                String senderId = visitorData.getString("token");
                LOG.info("[{}] customFields: [{}]", senderId, customFields);

                JSONArray messages = requestObject.getJSONArray("messages");
                JSONObject messageObj = messages.getJSONObject(0);
                if (messageObj.has("fileUpload")) {
                    JSONObject fileUploadObj = messageObj.getJSONObject("fileUpload");
                    String fileType = fileUploadObj.getString("type");
                    MediaType mediaType = MediaType.IMAGE;
                    if (fileType.contains("video")) {
                        mediaType = MediaType.VIDEO;
                    } else {
                        // Consider it as image. TODO: later if possible identify other types
                        mediaType = MediaType.IMAGE;
                    }
                    // normal text message
                    if (customFields.has(CustomFields.callbackUrl + "") && customFields.has(CustomFields.isGuest + "")) {
                        String url = customFields.getString(CustomFields.callbackUrl + "") + "callback/mediamessage/push/";
                        PushMessage msgPayload = new PushMessage();
//                        msgPayload.setGuest(customFields.getBoolean(CustomFields.isGuest + ""));
                        msgPayload.setGuest(true);
                        msgPayload.setUrlType(mediaType.toString());
                        msgPayload.setUrl(fileUploadObj.getString("publicFilePath"));
                        List<String> receipients = new ArrayList<>();
                        receipients.add(senderId);
                        msgPayload.setRecipientIds(receipients);
                        msgPayload.setRefId(senderId);
                        msgPayload.setSubTitle(agentName);
                        msgPayload.setTitle(agentName);
                        LOG.info("Agent sent media message in chat, url to be called is: [{}] with media [{}]", url, msgPayload.getUrl());
                        LOG.debug("[{}] Sending message: [{}]", senderId, msgPayload.toString());
                        channelInterfaceService.sendMessage(msgPayload, url, senderId, msgPayload.isGuest());
                    }
                } else {
                    if (customFields.has(CustomFields.callbackUrl + "") && customFields.has(CustomFields.isGuest + "")) {
                        // closing message
                        String url;
                        if (messageObj.has("msg") && "closing".equalsIgnoreCase(messageObj.getString("msg"))) {

                            url = customFields.getString(CustomFields.callbackUrl + "") + "conversation/handle/";
                            LOG.info("Agent closed the chat, url to be called is: [{}]", url);
                        } else {
                            // normal text message
                            url = customFields.getString(CustomFields.callbackUrl + "") + "callback/textmessage/push/";
                            LOG.info("Agent sent message in chat, url to be called is: [{}]", url);
                        }
//                        LOG.debug("is guest: [{}]", customFields.getString(CustomFields.isGuest+""));
                        PushMessage msgPayload = new PushMessage();
//                        msgPayload.setGuest(Boolean.parseBoolean(customFields.getString(CustomFields.isGuest+"")));
                        msgPayload.setGuest(true);
                        msgPayload.setMessage(messageObj.getString("msg"));
                        List<String> receipients = new ArrayList<>();
                        receipients.add(senderId);
                        msgPayload.setRecipientIds(receipients);
                        msgPayload.setRefId(senderId);
                        msgPayload.setSubTitle(agentName);
                        msgPayload.setTitle(agentName);
                        LOG.debug("[{}] Sending message: [{}]", senderId, msgPayload.toString());
                        channelInterfaceService.sendMessage(msgPayload, url, senderId, msgPayload.isGuest());

                    }
                }

            }
        } catch (Exception ex) {
            LOG.error("Processing of take handover failed ", ex);
            return ResponseEntity.status(HttpStatus.OK).build();
        }
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
