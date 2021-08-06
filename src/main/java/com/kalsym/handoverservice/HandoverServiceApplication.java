package com.kalsym.handoverservice;

import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import com.kalsym.handoverservice.models.*;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 1. Handover service receives inbound messages from channel wrappers. 2. It
 * validates and verifies the incoming message and forwards message to live
 * agents (if any one is available) 3. It receives outbound messages from live
 * agent and forwards to respective wrapper
 *
 * 4. It also periodically checks agents availability. Forwards offline message
 * if supported by agents interface
 *
 * @author z33Sh
 */
@SpringBootApplication
@EnableScheduling
//@EnableMongoAuditing
public class HandoverServiceApplication {

    @Value("${build.version:not-known}")
    String version;

    private static final Logger LOG = LoggerFactory.getLogger("application");
    // to store new customer chats for handling dangling converations. Service should remove entry from this hashmap if no response from agent/RC for a fixed time. And also on agent/RC reply
    public static ConcurrentHashMap<String, DanglingData> newCustomerChats;
    public static ConcurrentHashMap<String, DanglingData> conversationsLastMessageTime;
    public static ConcurrentHashMap<String, DanglingData> customerResponseAwaitQueue;

    public static void main(String[] args) {
        SpringApplication.run(HandoverServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner lookup(ApplicationContext context) {
        return args -> {
            VersionHolder.VERSION = version;

            LOG.info("[v{}][{}] {}", VersionHolder.VERSION, "", "\n"
                    + "                                               \n"
                    + ",--. ,--.        ,--.                          \n"
                    + "|  .'   / ,--,--.|  | ,---.,--. ,--.,--,--,--. \n"
                    + "|  .   ' ' ,-.  ||  |(  .-' \\  '  / |        | \n"
                    + "|  |\\   \\\\ '-'  ||  |.-'  `) \\   '  |  |  |  | \n"
                    + "`--' '--' `--`--'`--'`----'.-'  /   `--`--`--' \n"
                    + "                           `---'               "
                    + " :: com.kalsym ::              (v" + VersionHolder.VERSION + ")");

            newCustomerChats = new ConcurrentHashMap<>();
            conversationsLastMessageTime = new ConcurrentHashMap<>();
            customerResponseAwaitQueue = new ConcurrentHashMap<>();
        };
    }

    @Bean
    public RestTemplate getRestTemplate() {
        return new RestTemplate();
    }

}
