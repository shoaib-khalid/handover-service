package com.kalsym.handoverservice.controllers;

import com.kalsym.handoverservice.HandoverServiceApplication;
import com.kalsym.handoverservice.VersionHolder;
import com.kalsym.handoverservice.agent.models.VisitorPayload;
import com.kalsym.handoverservice.models.*;
import com.kalsym.handoverservice.agent.models.*;
import com.kalsym.handoverservice.enums.MediaType;
import com.kalsym.handoverservice.repositories.RoomsRepostiory;
import com.kalsym.handoverservice.services.AgentInterfaceService;
import com.kalsym.handoverservice.services.ChannelInterfaceService;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.json.JSONArray;
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
    private static Environment env;

    @Autowired
    private AgentInterfaceService agentInterfaceService;
    @Autowired
    private static ChannelInterfaceService channelInterfaceService;

    @Autowired
    private static RoomsRepostiory roomsRepository;

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
                String enableConfirmationQueueMonitor = env.getProperty("enable.confirmation.queue.monitor", "no");
                if ("YES".equalsIgnoreCase(enableConfirmationQueueMonitor)) {
                    if (null != HandoverServiceApplication.customerResponseAwaitQueue.get(senderId)) {
                        if ("NO".equalsIgnoreCase(requestBody.getData().trim())) {
                            LOG.info("[v{}] [{}]customer does not want to continue chat with agent, close chat with agent", VersionHolder.VERSION, senderId);
                            String url = requestBody.getCallbackUrl() + "callback/conversation/handle/";
                            String agentName = env.getProperty("default.agent.name", "HS");
                            String message = env.getProperty("customer.reject.confirmation.message", "Thank you for confirmation. you are no longer chatting with agent");
                            closeChat(senderId, url, agentName, message);
                            HandoverServiceApplication.customerResponseAwaitQueue.remove(senderId);
                        }
                    }
                }
                // 1 - Register/update Visitor
                List<CustomField> customFields = new ArrayList<>();
                customFields.add(new CustomField(CustomFields.callbackUrl, requestBody.getCallbackUrl(), true));
                customFields.add(new CustomField(CustomFields.data, requestBody.getData(), true));
                customFields.add(new CustomField(CustomFields.isGuest, requestBody.getIsGuest() + "", true));
                customFields.add(new CustomField(CustomFields.msgId, requestBody.getMsgId(), true));
                customFields.add(new CustomField(CustomFields.referral, requestBody.getReferral(), true));
//                String roomId = refrenceId + "r";
                String roomId = senderId;
                String token = senderId;

                Visitor visitor = new Visitor(senderId, token, "", customFields);
                VisitorPayload visitorPayload = new VisitorPayload(visitor);
                JSONObject visitorRegistrationResponse = agentInterfaceService.registerOrUpdateVisitor(visitorPayload, token);
                boolean isVisitorRegistrationSuccess = visitorRegistrationResponse.getBoolean("success");
                LOG.info("[{}] [{}] is visitor registration Success:  [{}] ", VersionHolder.VERSION, senderId, isVisitorRegistrationSuccess);
                if (isVisitorRegistrationSuccess) {

                    //  - Create/Update Room
                    JSONObject roomCreationResponse = agentInterfaceService.createOrUpdateRoom(token, roomId, refrenceId);
                    boolean isRoomCreationSuccesss = roomCreationResponse.getBoolean("success");
                    LOG.info("[{}] [{}] /:  [{}] ", VersionHolder.VERSION, senderId, isVisitorRegistrationSuccess);
                    if (isRoomCreationSuccesss) {
                        // - Send customer message to agent interface

                        Message msg = new Message(token, roomId, requestBody.getData());
//                        JSONObject msg = new JSONObject("{\n"
//                                + "  \"token\": \"" + token + "\",\n"
//                                + "  \"rid\": \"" + roomId + "\",\n"
//                                + "  \"msg\": \"" + requestBody.getData() + "\"\n"
//                                + "}");
//                        message.put("token", token);
//                        message.put("rid", roomId);
//                        message.put("msg", requestBody.getData());

                        JSONObject sendMessageResponse = agentInterfaceService.sendMessage(msg, refrenceId);
                        boolean isSendMessageSuccesss = sendMessageResponse.getBoolean("success");
                        LOG.info("[{}] [{}] is message sent Successfully:  [{}] ", VersionHolder.VERSION, senderId, isSendMessageSuccesss);

                        // Do add conversation in new customer chats - not ideal solution, later do check if room is new or old
                        String enableNewChatMonitor = ConfigReader.environment.getProperty("enable.new.chat.monitor", "no");
                        if ("YES".equalsIgnoreCase(enableNewChatMonitor)) {

                            DanglingData dd = new DanglingData(System.currentTimeMillis(), requestBody.getCallbackUrl());
                            HandoverServiceApplication.newCustomerChats.put(senderId, dd);

                            try {
                                int wait = 0;
                                int intermitentSleep = 5;
                                int cummulativeWait = env.getProperty("max_wait_in_seconds_for_agent_response_before_rejecting", Integer.class, 300);
                                do {
                                    wait = wait + intermitentSleep;
                                    Thread.sleep(intermitentSleep * 1000);
                                    if (null == HandoverServiceApplication.newCustomerChats.get(senderId)) {
                                        LOG.debug("[{}] [{}] agent already replied to chat, so exit loop ", VersionHolder.VERSION, senderId);
                                        break;
                                    }

                                } while (wait < cummulativeWait);
                                // check if chat still exists 
                                if (null != HandoverServiceApplication.newCustomerChats.get(senderId)) {
                                    HandoverServiceApplication.newCustomerChats.remove(senderId);

                                    LOG.debug("[{}] [{}] agent  not replied to chat or no agent is available, intimate user ", VersionHolder.VERSION, senderId);
                                    String rejectChatMessage = env.getProperty("message_for_reject_chat_for_no_agent_available", "No agent is available, Please try again later");
                                    String url = requestBody.getCallbackUrl() + "callback/conversation/handle/";
                                    closeChat(senderId, url, env.getProperty("default.agent.name", "HS"), rejectChatMessage);
                                }
                            } catch (Exception ex) {
                                LOG.error("[{}] [{}] Exception: [{}]", VersionHolder.VERSION, senderId, ex);
                            }
                        }

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
    public ResponseEntity<Void> outbound(@RequestBody String requestData) {
        try {
            LOG.info("requestData: [{}]", requestData);

            JSONObject requestObject = new JSONObject(requestData);

            LOG.info("requestObject: [{}]", requestObject);
            JSONObject visitorData = requestObject.getJSONObject("visitor");
            JSONObject customFields = visitorData.getJSONObject("customFields");
            String agentName = "agent";
            if (requestObject.has("agent")) {
                agentName = requestObject.getJSONObject("agent").getString("username");
            }
            String senderId = visitorData.getString("token");
            try {
                // update agent message time and remove from customer confirmation
                String enableDanglingChatDetector = env.getProperty("enable.dangling.chat.detector", "no");
                if ("YES".equalsIgnoreCase(enableDanglingChatDetector)) {
                    if (customFields.has(CustomFields.callbackUrl + "")) {
                        String callbackUrl = customFields.getString(CustomFields.callbackUrl + "");
                        DanglingData dd = new DanglingData(System.currentTimeMillis(), callbackUrl);
                        HandoverServiceApplication.conversationsLastMessageTime.put(senderId, dd);
                    }
                }
                String enableConfirmationQueueMonitor = env.getProperty("enable.confirmation.queue.monitor", "no");
                if ("YES".equalsIgnoreCase(enableConfirmationQueueMonitor)) {
                    HandoverServiceApplication.customerResponseAwaitQueue.remove(senderId);
                }

                // check if new chat exist, since agent replied, delete entry from hm
                String enableNewChatMonitor = env.getProperty("enable.new.chat.monitor", "no");
                if ("YES".equalsIgnoreCase(enableNewChatMonitor)) {
                    if (null != HandoverServiceApplication.newCustomerChats.get(senderId)) {
                        HandoverServiceApplication.newCustomerChats.remove(senderId);
                    }
                }
            } catch (Exception ex) {

            }
            LOG.info("[{}] customFields: [{}]", senderId, customFields);
            if (requestObject.has("type") && "Message".equalsIgnoreCase(requestObject.getString("type")) && requestObject.has("messages")) {

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

                            url = customFields.getString(CustomFields.callbackUrl + "") + "callback/conversation/handle/";
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

            } else if (requestObject.has("type") && "LivechatSession".equalsIgnoreCase(requestObject.getString("type")) && requestObject.has("closer") && requestObject.has("closedAt")) {
                String message = "chat closed by agent";
                String url = customFields.getString(CustomFields.callbackUrl + "") + "callback/conversation/handle/";
                closeChat(senderId, url, agentName, message);
            }
        } catch (Exception ex) {
            LOG.error("Processing of take handover failed ", ex);
            return ResponseEntity.status(HttpStatus.OK).build();
        }
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    public static void closeChat(String senderId, String url, String agentName, String message) {
        try {
            roomsRepository.deleteByFname(senderId);
            LOG.info("[{}] Delete room from mongo without waiting for response", senderId);
        } catch (Exception ex) {
            LOG.error("Error deleting room ", ex);
        }
        // closing message
        try {

            LOG.info("Closing the chat, url to be called is: [{}]", url);
            PushMessage msgPayload = new PushMessage();
            msgPayload.setGuest(true);
            msgPayload.setMessage(message);
            List<String> receipients = new ArrayList<>();
            receipients.add(senderId);
            msgPayload.setRecipientIds(receipients);
            msgPayload.setRefId(senderId);
            msgPayload.setSubTitle(agentName);
            msgPayload.setTitle(agentName);
            LOG.debug("[{}] Sending message: [{}]", senderId, msgPayload.toString());
            channelInterfaceService.sendMessage(msgPayload, url, senderId, msgPayload.isGuest());
        } catch (Throwable ex) {
            LOG.error("Error sending request to wrapper", ex);
        }
    }

    public static String sendMessageToWrapper(String senderId, String url, String message) {
        String response = "";
        try {

            LOG.info("Closing the chat, url to be called is: [{}]", url);
            PushMessage msgPayload = new PushMessage();
            msgPayload.setGuest(true);
            msgPayload.setMessage(message);
            List<String> receipients = new ArrayList<>();
            receipients.add(senderId);
            msgPayload.setRecipientIds(receipients);
            msgPayload.setRefId(senderId);
            String agentName = env.getProperty("default.agent.name", "HS");
            msgPayload.setSubTitle(agentName);
            msgPayload.setTitle(agentName);
            LOG.debug("[{}] Sending message: [{}]", senderId, msgPayload.toString());
            response = channelInterfaceService.sendMessage(msgPayload, url, senderId, msgPayload.isGuest());
        } catch (Throwable ex) {
            response = "Exception";
            LOG.error("Error sending request to wrapper", ex);
        }
        return response;
    }
}
