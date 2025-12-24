package org.example.ariesbackendweb.common.api;

import lombok.AllArgsConstructor;
import org.example.ariesbackendweb.MEC.DTOs.LaunchAgentTestDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Classe pour la communication avec l'agent, exploitera le repository/apiClient
 * */
@Service
@AllArgsConstructor
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);
    @Value("${agent.url}")
    private String agentUrl;

    private RestClient restClient;
    
    public AgentService (){
        this.restClient = RestClient.builder()
                .baseUrl(agentUrl)
                .build();
    }


    public void launchMecTestAgent(LaunchAgentTestDto data) throws IOException {
        MultiValueMap<String, Object> body = getBody(data);

        Map<?, ?> response = restClient.post()
                .uri(agentUrl + "/mec/launch")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(Map.class);

        assert response != null;
        log.info(response.toString());
    }

    private static MultiValueMap<String, Object> getBody(LaunchAgentTestDto datas) throws IOException {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        MultipartFile file = datas.getFile();
        ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };
        body.add("file", fileResource);
        body.add("sessionId", datas.getSessionId().toString());
        body.add("dHost", datas.getDHost());
        body.add("rHost", datas.getRHost());
        body.add("dPath", datas.getDPath());
        body.add("rPath", datas.getRPath());
        body.add("dShareName", datas.getDShareName());
        body.add("rShareName", datas.getRShareName());
        return body;
    }
}
