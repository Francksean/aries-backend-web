package org.example.ariesbackendweb.MWS.dtos.agent;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
public class MwsAgentTestRequestDto {


        private String sessionId;

        private String serviceName;

        private String wsdlUrl;

        private String endpointUrl;

        private String operationName;

        private String requestBody;

        private String username;

        private String password;

        private int timeoutMillis = 60000;

        private LocalDateTime createdAt;

        private String submittedBy;

        private String environment;

        private Map<String, String> metadata = new HashMap<>();


}
