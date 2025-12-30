package org.example.ariesbackendweb.MWS.dtos;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO de réponse contenant un programme MWS.
 * Dans l'approche hybride, nous retournons juste les noms des opérations,
 * pas leurs détails complets.
 */
@Data
public class MwsProgramResponse {

    private String id;
    private String programName;
    private String description;
    private String programType;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime lastSyncedAt;

    /**
     * Endpoints par environnement.
     */
    private Map<String, EndpointInfo> endpoints;

    /**
     * Tags du programme.
     */
    private List<String> tags;

    /**
     * Liste simple des noms d'opérations.
     * Les détails (templates, paramètres) sont obtenus via d'autres endpoints.
     */
    private List<String> operations;

    @Data
    public static class EndpointInfo {
        private String wsdlUrl;
        private String endpointUrl;
    }
}