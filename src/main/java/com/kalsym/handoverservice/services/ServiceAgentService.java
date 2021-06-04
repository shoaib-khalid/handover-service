package com.kalsym.handoverservice.services;

import com.kalsym.handoverservice.VersionHolder;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author z33Sh
 */
@Service
public class ServiceAgentService {

    private static final Logger LOG = LoggerFactory.getLogger("application");

    @Value("${service.agent.url:http://209.58.160.20:20921/clients/}")
    String serviceAgentUrl;

    public JSONObject getAgents(String roleId, String storeId, String refId) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("AccessToken");
        HttpEntity entity = new HttpEntity(headers);
        String url = serviceAgentUrl + "?roleId=" + roleId + "&storeId=" + storeId;
        LOG.debug("[v{}][{}] {} ", VersionHolder.VERSION, refId, "url to be called: " + serviceAgentUrl);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        LOG.debug("[v{}][{}] {}", VersionHolder.VERSION, refId, "response: " + response);
        return new JSONObject(response.getBody());
    }
    
    
}
