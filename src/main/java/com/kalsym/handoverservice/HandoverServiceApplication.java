package com.kalsym.handoverservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

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
public class HandoverServiceApplication {

    @Value("${build.version:not-known}")
    String version;

    private static final Logger LOG = LoggerFactory.getLogger("application");

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
        };
    }

    @Bean
    public RestTemplate getRestTemplate() {
        return new RestTemplate();
    }

}
