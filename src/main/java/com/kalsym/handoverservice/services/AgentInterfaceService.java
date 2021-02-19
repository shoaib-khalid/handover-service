package com.kalsym.handoverservice.services;

import com.kalsym.handoverservice.VersionHolder;
import com.kalsym.handoverservice.agent.models.Message;
import com.kalsym.handoverservice.agent.models.VisitorPayload;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author z33Sh
 */
@Service
public class AgentInterfaceService {

    private static final Logger LOG = LoggerFactory.getLogger("application");

    @Value("${agent.interface.url:http://209.58.160.20:3000/}")
    String agentInterfaceUrl;

    public JSONObject registerOrUpdateVisitor(VisitorPayload payload, String refId) throws Exception {
        LOG.debug("[v{}][{}] {} ", VersionHolder.VERSION, refId, "agentInterfaceUrl: " + agentInterfaceUrl);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.postForEntity(agentInterfaceUrl + "/api/v1/livechat/visitor", payload, String.class);
        LOG.debug("[v{}][{}] {}", VersionHolder.VERSION, refId, "response: " + response.getBody());
        return new JSONObject(response.getBody());
    }
    public JSONObject sendMessage(Message payload, String refId) throws Exception {
        LOG.debug("[v{}][{}] {} ", VersionHolder.VERSION, refId, "agentInterfaceUrl: " + agentInterfaceUrl);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.postForEntity(agentInterfaceUrl + "/api/v1/livechat/message", payload, String.class);
        LOG.debug("[v{}][{}] {}", VersionHolder.VERSION, refId, "response: " + response.getBody());
        return new JSONObject(response.getBody());
    }

    public JSONObject createOrUpdateRoom(String token, String roomId, String refId) throws Exception {
        LOG.debug("[v{}][{}] {} ", VersionHolder.VERSION, refId, "agentInterfaceUrl: " + agentInterfaceUrl);
        RestTemplate restTemplate = new RestTemplate();
        Map<String, String> params = new HashMap<>();
        params.put("token", token);
        params.put("rid", roomId);
        String response = restTemplate.getForObject(agentInterfaceUrl + "/api/v1/livechat/room?token="+token+"&rid="+roomId, String.class);
        LOG.debug("[v{}][{}] {}", VersionHolder.VERSION, refId, "response: " + response);
        return new JSONObject(response);
    }
}
