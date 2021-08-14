package com.kalsym.handoverservice.controllers;

import com.kalsym.handoverservice.HandoverServiceApplication;
import com.kalsym.handoverservice.VersionHolder;
import com.kalsym.handoverservice.agent.models.VisitorPayload;
import com.kalsym.handoverservice.models.*;
import com.kalsym.handoverservice.agent.models.*;
import com.kalsym.handoverservice.config.ConfigReader;
import com.kalsym.handoverservice.enums.MediaType;
import com.kalsym.handoverservice.repositories.RoomsRepostiory;
import com.kalsym.handoverservice.repositories.UserRepository;
import com.kalsym.handoverservice.services.AgentInterfaceService;
import com.kalsym.handoverservice.services.ChannelInterfaceService;
import com.kalsym.handoverservice.services.FlowCoreService;
import com.kalsym.handoverservice.services.StoresService;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author z33Sh
 */
@RestController
@RequestMapping("/")
public class MessageController {

    private static final Logger LOG = LoggerFactory.getLogger("application");
    @Autowired
    private AgentInterfaceService agentInterfaceService;
    @Autowired
    private StoresService storesService;
    @Autowired
    private FlowCoreService flowCoreService;

    @Autowired
    private ChannelInterfaceService channelInterfaceService;

    @Autowired
    private RoomsRepostiory roomsRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Endpoint for receiving customer messages from different channel wrappers.
     * Validates and forward the incoming message to live agents interface.
     *
     * @param request
     * @return
     */
    @GetMapping(path = {"select/agent"}, name = "select-agent-get")
    public ResponseEntity<?> selectAgent(HttpServletRequest request,
            @RequestParam String referenceId,
            @RequestParam String refId) {
//        LOG.info(request.getQueryString()+"", VersionHolder.VERSION);
        LOG.info("[v{}] Request received for select/agent", VersionHolder.VERSION);
        JSONObject storeObject = new JSONObject();
        try {
            storeObject = flowCoreService.getStoreId(referenceId, refId);

            String storeId = storeObject.getJSONObject("data").get("storeId").toString();
            String USER_SERVICE_URL = ConfigReader.environment.getProperty("user.service.url", "http://209.58.160.20:1201/clients/?storeId=") + storeId;
            RestTemplate getAgentsFromStoreRequest = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Bearer accessToken");
            HttpEntity<String> body = new HttpEntity<>(headers);
            ResponseEntity<String> response = getAgentsFromStoreRequest.exchange(USER_SERVICE_URL, HttpMethod.GET, body, String.class);
            JSONObject responseBody = new JSONObject(response.getBody());
            System.out.println("--------------------------------------------");
            System.out.println(responseBody);

            JSONArray agentListRaw = responseBody.getJSONObject("data").getJSONArray("content");
            List<JSONObject> agentList = new ArrayList<>();
            for (int i = 0; i < agentListRaw.length(); i++) {
                agentList.add(agentListRaw.getJSONObject(i));
            }
            List<JSONObject> filteredAgents = agentList.stream()
                    .filter(a -> a.getString("roleId").equals("STORE_CSR_ADMIN") || a.getString("roleId").equals("STORE_CSR_COMPLAINT")).collect(Collectors.toList());

            System.out.println("______________________FILTERED AGENTS____________________________");
            System.out.println(filteredAgents);

            List<User> onlineAgents = new ArrayList<>();
            for (JSONObject jsonAgent : filteredAgents) {
                Optional<User> optUser = userRepository.findByUsername(jsonAgent.getString("username"));
                if (optUser.isPresent()) {
                    onlineAgents.add(optUser.get());
                }
            }

            System.out.println("_____________________ONLINE AGENTS_____________________________");
            System.out.println(onlineAgents);

            JSONObject agent = new JSONObject();
            if (onlineAgents.size() == 1) {
                agent.put("_id", onlineAgents.get(0).id);
                agent.put("username", onlineAgents.get(0).username);
            } else if (onlineAgents.size() > 1) {
                agent.put("_id", onlineAgents.get(new Random().nextInt(onlineAgents.size())).id);
                agent.put("username", onlineAgents.get(new Random().nextInt(onlineAgents.size())).username);
            } else {
                String agentUserName = ConfigReader.environment.getProperty("livechat.default.agent.username", "csr-router");
                String agentId = ConfigReader.environment.getProperty("livechat.default.agent.id", "M2bNGEH27wT5fHEp4");
                agent.put("_id", agentId);
                agent.put("username", agentUserName);
//            System.err.println(getStoreName("105350328414803", "2323423"));
            }
            LOG.info("[v{}] Return agent: {}", VersionHolder.VERSION, agent);
            return ResponseEntity.status(HttpStatus.OK).body(agent.toString());
        } catch (Exception e) {
            LOG.error("Exception :{}", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Server Error");
        }
    }

    /**
     *
     * @param referenceId
     * @param refId
     * @return
     */
    private String getStoreName(String referenceId, String refId) {
        String storeName = "";
        String storeId = "";

        try {
// Get storeId from flow-core service

            JSONObject response = flowCoreService.getStoreId(referenceId, refId);
            storeId = response.getJSONObject("data").getString("storeId");
            LOG.info("[v{}][{}] Store ID from flow core: {} against referenceId: {}", VersionHolder.VERSION, refId, storeId, referenceId);
            // Get store name from store service
            JSONObject storeObject = storesService.getStore(storeId, refId);
            storeName = storeObject.getJSONObject("data").getString("name");
            LOG.info("[v{}][{}] Store name from store service: [{}] against storeId: {}", VersionHolder.VERSION, refId, storeName, storeId);

        } catch (Exception ex) {
            LOG.error("no store name found with referenceId  {} and ex:{}", referenceId, ex);
        }
        return storeName;
    }
//    /**
//     *
//     * @param referenceId
//     * @param refId
//     * @return
//     */
//    private Agent getCSRAgent(String referenceId, String refId) {
//        JSONObject resultAgent = new JSONObject();
//        try {
//
//            String storeId = "";
//            JSONArray agentsArray = null;
//
//            try {// Get storeId from flow-core service
//
//                JSONObject response = flowCoreService.getStoreId(referenceId, refId);
//                storeId = response.getJSONObject("data").getString("storeId");
//                LOG.info("[v{}][{}] Store ID from flow core: {} against referenceId: {}", VersionHolder.VERSION, refId, storeId, referenceId);
//                // Get agents ids from service agent
//                JSONObject agentsObject = serviceAgentService.getAgents("STORE_CSR_COMPLAINT", storeId, refId);
//                agentsArray = agentsObject.getJSONObject("data").getJSONArray("content");
//            } catch (Exception ex) {
//                System.out.println("no store found with referenceId ");
//            }
//            String agentUserName = ConfigReader.environment.getProperty("livechat.default.agent.username", "zeeshan-ks");
//            String agentId = ConfigReader.environment.getProperty("livechat.default.agent.id", "nubj4bBZHctboNnXt");
//            if (null == agentsArray) {
//                // No agents for store found, send message to default agent
//                LOG.info("[v{}][{}] Using default agent ID:{} agentUsername: {} because not found agents in from storeId: {}", VersionHolder.VERSION, refId, agentId, agentUserName, storeId);
//            } else if (agentsArray.length() == 1) {
//                // Only one agent exist, send message 
//                JSONObject obj = agentsArray.getJSONObject(0);
//                agentUserName = obj.getString("username");
//                agentId = obj.getString("liveChatAgentId");
//                LOG.info("[v{}][{}] Found agent ID:{} agentUsername: {} from storeId: {}", VersionHolder.VERSION, refId, agentId, agentUserName, storeId);
//
//            } else if (agentsArray.length() > 1) {
//                // More than one agent found, choose random agent
//                Random rand = new Random();
//                int randomAgentIndex = rand.nextInt(agentsArray.length());
//                JSONObject obj = agentsArray.getJSONObject(randomAgentIndex);
//                agentUserName = obj.getString("username");
//                agentId = obj.getString("liveChatAgentId");
//                LOG.info("[v{}][{}] Selected random agent ID:{} agentUsername: {} from storeId: {}", VersionHolder.VERSION, refId, agentId, agentUserName, storeId);
//            }
//            resultAgent.put("agentId", agentId);
//            resultAgent.put("agentUserName", agentUserName);
//
//        } catch (Exception ex) {
//            System.out.println(ex);
//            LOG.warn("Processing  failed: {}", ex);
//            return null;
//        }
//
//        return new Agent(resultAgent.getString("agentId"), resultAgent.getString("agentUserName"));
//    }

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
        String ref = "";
        try {

            LOG.info("[v{}] Request received from [{}]  with {}", VersionHolder.VERSION, senderId, "queryString: " + request.getQueryString());
            if (null != requestBody && null != requestBody.getReferenceId()) {
                String referenceId = requestBody.getReferenceId();
                ref = referenceId;

                LOG.info("[v{}][{}] {}", VersionHolder.VERSION, senderId, "body: " + requestBody.toString());
                String enableConfirmationQueueMonitor = ConfigReader.environment.getProperty("enable.confirmation.queue.monitor", "no");
                if ("YES".equalsIgnoreCase(enableConfirmationQueueMonitor)) {
                    if (null != HandoverServiceApplication.customerResponseAwaitQueue.get(senderId)) {
                        if ("NO".equalsIgnoreCase(requestBody.getData().trim())) {
                            LOG.info("[v{}] [{}]customer does not want to continue chat with agent, close chat with agent", VersionHolder.VERSION, senderId);
                            String url = requestBody.getCallbackUrl() + "callback/conversation/handle/";
                            String agentName = ConfigReader.environment.getProperty("default.agent.name", "HS");
                            String message = ConfigReader.environment.getProperty("customer.reject.confirmation.message", "Thank you for confirmation. you are no longer chatting with agent");
                            closeChat(senderId, url, agentName, message, referenceId, roomsRepository, channelInterfaceService);
                            HandoverServiceApplication.customerResponseAwaitQueue.remove(senderId);
                        }
                    }
                }
                String storeName = getStoreName(referenceId, refrenceId);
                // 1 - Register/update Visitor
                List<CustomField> customFields = new ArrayList<>();
                customFields.add(new CustomField(CustomFields.callbackUrl, requestBody.getCallbackUrl(), true));
                customFields.add(new CustomField(CustomFields.data, requestBody.getData(), true));
                customFields.add(new CustomField(CustomFields.isGuest, requestBody.getIsGuest() + "", true));
                customFields.add(new CustomField(CustomFields.msgId, requestBody.getMsgId(), true));
                customFields.add(new CustomField(CustomFields.referral, requestBody.getReferral(), true));
                customFields.add(new CustomField(CustomFields.referenceId, referenceId, true));
                customFields.add(new CustomField(CustomFields.refId, refrenceId, true));
                customFields.add(new CustomField(CustomFields.storeName, storeName, true));
//                String roomId = referenceId + "r";
                String roomId = senderId;
                String token = senderId;

                Visitor visitor = new Visitor(senderId, token, "", customFields);
                VisitorPayload visitorPayload = new VisitorPayload(visitor);
                JSONObject visitorRegistrationResponse = agentInterfaceService.registerOrUpdateVisitor(visitorPayload, token);
                boolean isVisitorRegistrationSuccess = visitorRegistrationResponse.getBoolean("success");
                LOG.info("[{}] [{}] is visitor registration Success:  [{}] ", VersionHolder.VERSION, senderId, isVisitorRegistrationSuccess);
                if (isVisitorRegistrationSuccess) {

                    //  - Create/Update Room
                    JSONObject roomCreationResponse = agentInterfaceService.createOrUpdateRoom(token, roomId, referenceId);
                    boolean isRoomCreationSuccesss = roomCreationResponse.getBoolean("success");
                    LOG.info("[{}] [{}] /:  [{}] ", VersionHolder.VERSION, senderId, isVisitorRegistrationSuccess);
                    if (isRoomCreationSuccesss) {
                        // - Send customer message to agent interface
//                        Agent agent = getCSRAgent(referenceId, senderId);
                        Message msg;
//                        if (null == agent) {
                        msg = new Message(token, roomId, requestBody.getData());
//                        } else {
//                            msg = new Message(token, roomId, requestBody.getData(), agent);
//                        }

                        JSONObject sendMessageResponse = agentInterfaceService.sendMessage(msg, referenceId);
                        boolean isSendMessageSuccesss = sendMessageResponse.getBoolean("success");
                        LOG.info("[{}] [{}] is message sent Successfully:  [{}] with payload {}", VersionHolder.VERSION, senderId, isSendMessageSuccesss, msg);
                        new Thread(() -> {
                            // Do add conversation in new customer chats - not ideal solution, later do check if room is new or old
                            String enableNewChatMonitor = ConfigReader.environment.getProperty("enable.new.chat.monitor", "no");

                            if ("YES".equalsIgnoreCase(enableNewChatMonitor)) {
                                LOG.info("[{}] [{}] monitoring new chat started");

                                DanglingData dd = new DanglingData(System.currentTimeMillis(), requestBody.getCallbackUrl(), referenceId);
                                HandoverServiceApplication.newCustomerChats.put(senderId, dd);

                                try {
                                    int wait = 0;
                                    int intermitentSleep = 5;
                                    int cummulativeWait = ConfigReader.environment.getProperty("max_wait_in_seconds_for_agent_response_before_rejecting", Integer.class, 300);
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
                                        String rejectChatMessage = ConfigReader.environment.getProperty("message_for_reject_chat_for_no_agent_available", "No agent is available, Please try again later");
                                        String url = requestBody.getCallbackUrl() + "callback/conversation/handle/";
                                        closeChat(senderId, url, ConfigReader.environment.getProperty("default.agent.name", "HS"), rejectChatMessage, referenceId, roomsRepository, channelInterfaceService);
                                    }
                                } catch (Exception ex) {
                                    LOG.error("[{}] [{}] Exception: [{}]", VersionHolder.VERSION, senderId, ex);
                                }
                            }
                        }).start();

                    }
                }
            } else {
                LOG.warn("null or empty requestBody: {}", requestBody);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
        } catch (Exception ex) {

            try {
                if (null != ex.getMessage() && ex.getMessage().contains("no online agents") && null != requestBody) {
                    LOG.warn("[{}] [{}]  No all agents are unavailable at this moment, plz try later", VersionHolder.VERSION, senderId);
                    String rejectChatMessage = ConfigReader.environment.getProperty("message_for_reject_chat_for_no_agent_available", "No agent is available, Please try again later");
                    String urlHandle = requestBody.getCallbackUrl() + "callback/conversation/handle/";
                    String urlPush = requestBody.getCallbackUrl() + "callback/textmessage/push/";
                    sendMessageToWrapper(senderId, urlPush, rejectChatMessage, ref, channelInterfaceService, ConfigReader.environment.getProperty("default.agent.name", "HS"));
                    closeChat(senderId, urlHandle, ConfigReader.environment.getProperty("default.agent.name", "HS"), rejectChatMessage, ref, roomsRepository, channelInterfaceService);
                } else {
                    LOG.warn("Processing of inbound message failed: {}", ex);
                }
            } catch (Exception exp) {
                LOG.warn("Processing of inbound message failed: {}", exp);
            }
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
                agentName = requestObject.getJSONObject("agent").getString("name");
            }
            String senderId = visitorData.getString("token");
            try {
                // update agent message time and remove from customer confirmation
                String enableDanglingChatDetector = ConfigReader.environment.getProperty("enable.dangling.chat.detector", "no");
                if ("YES".equalsIgnoreCase(enableDanglingChatDetector)) {
                    if (customFields.has(CustomFields.callbackUrl + "")) {
                        String callbackUrl = customFields.getString(CustomFields.callbackUrl + "");
                        DanglingData dd = new DanglingData(System.currentTimeMillis(), callbackUrl, customFields.getString(CustomFields.referenceId + ""));
                        HandoverServiceApplication.conversationsLastMessageTime.put(senderId, dd);
                        LOG.debug("[{}]'s put in conversation last message", senderId);

                    }
                }
                String enableConfirmationQueueMonitor = ConfigReader.environment.getProperty("enable.confirmation.queue.monitor", "no");
                if ("YES".equalsIgnoreCase(enableConfirmationQueueMonitor)) {
                    HandoverServiceApplication.customerResponseAwaitQueue.remove(senderId);
                }

                // check if new chat exist, since agent replied, delete entry from hm
                String enableNewChatMonitor = ConfigReader.environment.getProperty("enable.new.chat.monitor", "no");
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
                    if (customFields.has(CustomFields.callbackUrl + "") && customFields.has(CustomFields.isGuest + "") && customFields.has(CustomFields.referenceId + "")) {
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
                        msgPayload.setReferenceId(customFields.getString(CustomFields.referenceId + ""));
                        msgPayload.setSubTitle(agentName);
                        msgPayload.setTitle(agentName);
                        LOG.info("Agent sent media message in chat, url to be called is: [{}] with media [{}]", url, msgPayload.getUrl());
                        LOG.debug("[{}] Sending message: [{}]", senderId, msgPayload.toString());
                        channelInterfaceService.sendMessage(msgPayload, url, senderId, msgPayload.isGuest());
                    }
                } else {
                    if (customFields.has(CustomFields.callbackUrl + "") && customFields.has(CustomFields.isGuest + "") && customFields.has(CustomFields.referenceId + "")) {
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
                        msgPayload.setReferenceId(customFields.getString(CustomFields.referenceId + ""));
                        msgPayload.setSubTitle(agentName);
                        msgPayload.setTitle(agentName);
                        LOG.debug("[{}] Sending message: [{}]", senderId, msgPayload.toString());
                        channelInterfaceService.sendMessage(msgPayload, url, senderId, msgPayload.isGuest());

                    }
                }

            } else if (requestObject.has("type") && "LivechatSession".equalsIgnoreCase(requestObject.getString("type")) && requestObject.has("closer") && requestObject.has("closedAt")) {
                String message = "chat closed by agent";
                String url = customFields.getString(CustomFields.callbackUrl + "") + "callback/conversation/handle/";
                String referenceId = customFields.getString(CustomFields.referenceId + "");
                closeChat(senderId, url, agentName, message, referenceId, roomsRepository, channelInterfaceService);
            }
        } catch (Exception ex) {
            LOG.error("Processing of take handover failed ", ex);
            return ResponseEntity.status(HttpStatus.OK).build();
        }
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /**
     *
     * @param senderId
     * @param url
     * @param agentName
     * @param message
     * @param referenceId
     * @param roomSvc
     * @param channelSvc
     */
    public static void closeChat(String senderId, String url, String agentName, String message, String referenceId, RoomsRepostiory roomSvc, ChannelInterfaceService channelSvc) {
        try {
            if ("yes".equalsIgnoreCase(ConfigReader.environment.getProperty("do_delete_room_from_rc_on_close_chat", "yes"))) {
                roomSvc.deleteByFname(senderId);
                LOG.info("[{}] Delete room from mongo without waiting for response", senderId);
            }

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
            msgPayload.setReferenceId(referenceId);
            msgPayload.setSubTitle(agentName);
            msgPayload.setTitle(agentName);
            LOG.debug("[{}] Sending message: [{}]", senderId, msgPayload.toString());
            channelSvc.sendMessage(msgPayload, url, senderId, msgPayload.isGuest());
        } catch (Throwable ex) {
            LOG.error("Error sending request to wrapper", ex);
        }
    }

    public static String sendMessageToWrapper(String senderId, String url, String message, String referenceId, ChannelInterfaceService channelSvc) {
        return sendMessageToWrapper(senderId, url, message, referenceId, channelSvc, ConfigReader.environment.getProperty("default.agent.name", "HS"));
    }

    public static String sendMessageToWrapper(String senderId, String url, String message, String referenceId, ChannelInterfaceService channelSvc, String agentName) {
        String response = "";
        try {

            LOG.info("Closing the chat, url to be called is: [{}]", url);
            PushMessage msgPayload = new PushMessage();
            msgPayload.setGuest(true);
            msgPayload.setMessage(message);
            List<String> receipients = new ArrayList<>();
            receipients.add(senderId);
            msgPayload.setRecipientIds(receipients);
            msgPayload.setRefId(referenceId);
            msgPayload.setReferenceId(referenceId);
//            String agentName = ConfigReader.environment.getProperty("default.agent.name", "HS");
            msgPayload.setSubTitle(agentName);
            msgPayload.setTitle(agentName);
            LOG.debug("[{}] Sending message: [{}]", senderId, msgPayload.toString());
            response = channelSvc.sendMessage(msgPayload, url, senderId, msgPayload.isGuest());
        } catch (Throwable ex) {
            response = "Exception";
            LOG.error("Error sending request to wrapper", ex);
        }
        return response;
    }
}
