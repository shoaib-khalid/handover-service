package com.kalsym.handoverservice.schedular;

import com.kalsym.handoverservice.HandoverServiceApplication;
import com.kalsym.handoverservice.config.ConfigReader;
import com.kalsym.handoverservice.controllers.MessageController;
import com.kalsym.handoverservice.utils.DateTimeUtil;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import com.kalsym.handoverservice.models.*;
import com.kalsym.handoverservice.repositories.RoomsRepostiory;
import com.kalsym.handoverservice.services.ChannelInterfaceService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author z33Sh
 */
/**
 * Configures the scheduler to allow multiple concurrent pools. Prevents
 * blocking.
 */
@Configuration
public class DanglingChatMonitor {

    private final static Logger LOG = LoggerFactory.getLogger("application");

//    private final String serverId = ConfigReader.environment.getProperty("server.id", "000");
    private final String enableDanglingChatDetector = ConfigReader.environment.getProperty("enable.dangling.chat.detector", "no");
    private final String enableConfirmationQueueMonitor = ConfigReader.environment.getProperty("enable.confirmation.queue.monitor", "no");

    public final int danglingChatQualifyMinutes = ConfigReader.environment.getProperty("dangling.chat.qualify.minutes", Integer.class, 15);
//    public final int waitQueueExhaustQualifyMinutes = ConfigReader.environment.getProperty("wait.queue.exhaust.qualify.minutes", Integer.class, 15);
    public final int customerConfirmationTimeoutMinutes = ConfigReader.environment.getProperty("customer.confirmation.timeout.minutes", Integer.class, 15);

    private final String danglingChatCustomerQueryMessage = ConfigReader.environment.getProperty("dangling.chat.customer.query.message", "Agents are offline, do you want to continue chating with agent?");
    private final String confirmationQueueTimeoutNessage = ConfigReader.environment.getProperty("confirmation.queue.timeout.message", "You session with live agent has ended");
    private static boolean isDanglingChatDetectorRunning = false;
    private static boolean isWaitQueueMonitorRunning = false;
    @Autowired
    private ChannelInterfaceService channelInterfaceService;

    @Autowired
    private RoomsRepostiory roomsRepository;

    /**
     * Detect dangling chats
     *
     */
    @Scheduled(cron = "${dangling.chat.detector.cron.job:15 * * * * *}")
    public void danglingChatDetector() {

        try {
            if ("YES".equalsIgnoreCase(enableDanglingChatDetector)) {
                if (!isDanglingChatDetectorRunning) {
                    isDanglingChatDetectorRunning = true;
                    LOG.info("dangling chat detector started. size:" + HandoverServiceApplication.conversationsLastMessageTime.size());
                    for (Map.Entry mapElement : HandoverServiceApplication.conversationsLastMessageTime.entrySet()) {
                        String senderId = (String) mapElement.getKey();
                        LOG.info("Processing for :" + senderId + "and data:" + mapElement.getValue());

//                        if (mapElement.getValue() instanceof DanglingData) {
                        DanglingData ddLastMessage = (DanglingData) mapElement.getValue();
                        int timeDifferenceInMinutes = DateTimeUtil.getTimeDifferenceInMinutesFromMilli(ddLastMessage.getTimeInMillis());
                        LOG.info("[{}]'s last mesage time diff:{}", senderId, timeDifferenceInMinutes);
                        if (timeDifferenceInMinutes > danglingChatQualifyMinutes && null == HandoverServiceApplication.customerResponseAwaitQueue.get(senderId)) {
                            LOG.info("[{}]'s has no agent message since {} minutes. Sending reminder to user", senderId, timeDifferenceInMinutes);
                            DanglingData ddAwaitResponse = new DanglingData(System.currentTimeMillis(), ddLastMessage.getCallBackUrl(), ddLastMessage.getRefrenceId());
                            HandoverServiceApplication.customerResponseAwaitQueue.put(senderId, ddAwaitResponse);
                            HandoverServiceApplication.conversationsLastMessageTime.remove(senderId);

                            // Send message to user
                            String url = ddLastMessage.getCallBackUrl() + "callback/textmessage/push/";
                            String sendMessageStatus = MessageController.sendMessageToWrapper(senderId, url, danglingChatCustomerQueryMessage, ddLastMessage.getRefrenceId(), channelInterfaceService);
                            LOG.info("[{}]'s message sent [{}] to wrapper url [{}] time diff:[{}] and status:[{}]", senderId, danglingChatCustomerQueryMessage, url, timeDifferenceInMinutes, sendMessageStatus);
                        }
//                        }
                    }
                    LOG.info("dangling chat detector finished");
                    isDanglingChatDetectorRunning = false;
                } else {
                    LOG.info("dangling chat detector already running");
                }
            }

        } catch (Exception ex) {
            LOG.error("Got exception in danglingChatDetector ", ex);
        }
    }

    /**
     * Detect dangling chats
     *
     */
    @Scheduled(cron = "${wait.queue.monitor.cron.job:10 * * * * *}")
    public void waitQueueMonitor() {

        try {
            if ("YES".equalsIgnoreCase(enableConfirmationQueueMonitor)) {
                if (!isWaitQueueMonitorRunning) {
                    isWaitQueueMonitorRunning = true;
                    LOG.info("wait queue monitor started. Size:" + HandoverServiceApplication.customerResponseAwaitQueue.size());
                    for (Map.Entry mapElement : HandoverServiceApplication.customerResponseAwaitQueue.entrySet()) {
                        String senderId = (String) mapElement.getKey();
                        if (mapElement instanceof DanglingData) {
                            DanglingData dd = (DanglingData) mapElement.getValue();
                            int timeDifferenceInMinutes = DateTimeUtil.getTimeDifferenceInMinutesFromMilli(dd.getTimeInMillis());
                            LOG.debug("[{}]'s last reply time diff:{}", senderId, timeDifferenceInMinutes);
                            if (timeDifferenceInMinutes > customerConfirmationTimeoutMinutes && null != HandoverServiceApplication.customerResponseAwaitQueue.get(senderId)) {
                                LOG.debug("[{}]'s has not replied to query since {} minutes. sending close chat request to wrapper", senderId, timeDifferenceInMinutes);

                                HandoverServiceApplication.customerResponseAwaitQueue.remove(senderId);
                                // Send close chat request to wrapper message
                                String url = dd.getCallBackUrl() + "callback/conversation/handle/";
                                String agentName = ConfigReader.environment.getProperty("default.agent.name", "HS");
                                MessageController.closeChat(senderId, url, agentName, confirmationQueueTimeoutNessage, dd.getRefrenceId(), roomsRepository, channelInterfaceService);
                                LOG.info("[{}]'s close message [{}] sent to wrapper at url[{}] time diff:[{}]", senderId, confirmationQueueTimeoutNessage, url, timeDifferenceInMinutes);
                            }
                        }
                    }
                    LOG.info("wait queue monitor finished");
                    isWaitQueueMonitorRunning = false;
                } else {
                    LOG.info("wait queue monitor already running");
                }
            }

        } catch (Exception ex) {
            LOG.error("Got exception in waitQueueMonitor ", ex);
        }
    }

}
