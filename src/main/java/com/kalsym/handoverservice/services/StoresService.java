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
public class StoresService {

    private static final Logger LOG = LoggerFactory.getLogger("application");

    @Value("${store.service.url:http://209.58.160.20:7071/stores/}")
    String serviceAgentUrl;

    public JSONObject getStore(String storeId, String refId) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("AccessToken");
        HttpEntity entity = new HttpEntity(headers);
        String url = serviceAgentUrl + storeId;
        LOG.debug("[v{}][{}] {} ", VersionHolder.VERSION, refId, "stores url to be called: " + url);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        LOG.debug("[v{}][{}] {}", VersionHolder.VERSION, refId, "response: " + response);
        return new JSONObject(response.getBody());
    }

}
