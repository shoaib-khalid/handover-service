package com.kalsym.handoverservice.services;

import com.kalsym.handoverservice.VersionHolder;
import com.kalsym.handoverservice.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author z33Sh
 */
@Service
public class ChannelInterfaceService {
   private static final Logger LOG = LoggerFactory.getLogger("application");


    public String sendMessage(PushMessage message, String url, String refId, boolean isGuest) throws Exception {
        String logprefix = refId;
        LOG.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "url: " + url);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.postForEntity(url, message, String.class);
        LOG.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "response: " + response.getBody());
        return response.getBody();
    }  
//    public String sendMessage(MediaMessage message, String url, String refId, boolean isGuest) throws Exception {
//        String logprefix = refId;
//        LOG.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "url: " + url);
//        RestTemplate restTemplate = new RestTemplate();
//        ResponseEntity<String> response = restTemplate.postForEntity(url, message, String.class);
//        LOG.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "response: " + response.getBody());
//        return response.getBody();
//    }  
}
