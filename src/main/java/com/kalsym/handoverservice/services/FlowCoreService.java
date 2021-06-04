package com.kalsym.handoverservice.services;

import com.kalsym.handoverservice.VersionHolder;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author z33Sh
 */
@Service
public class FlowCoreService {

    private static final Logger LOG = LoggerFactory.getLogger("application");

    @Value("${flow.core.interface.url:http://209.58.160.20:7313/flows/}")
    String flowCoreInterfaceUrl;

    public JSONObject getStoreId(String referenceId, String refId) throws Exception {
        LOG.debug("[v{}][{}] {} ", VersionHolder.VERSION, refId, "flowCoreInterfaceUrl: " + flowCoreInterfaceUrl);
        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.getForObject(flowCoreInterfaceUrl + "?botId=" + referenceId, String.class);
        LOG.debug("[v{}][{}] {}", VersionHolder.VERSION, refId, "response: " + response);
        return new JSONObject(response);
    }
}
