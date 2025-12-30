package org.example.ariesbackendweb.MWS.dtos;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * DTO pour la création ou mise à jour d'un programme MWS.
 */
@Data
public class MwsProgramRequest {

    private String programName;
    private String description;
    private String programType;

    /**
     * Endpoints par environnement.
     * Clé: environnement (DEV, TEST, PROD)
     * Valeur: informations WSDL et endpoint
     */
    private Map<String, EndpointInfo> endpoints;

    private List<String> tags;

    /**
     * Si true, déclenche une synchronisation automatique après création.
     */
    private boolean autoSync = true;

    /**
     * Environnement à utiliser pour la synchronisation initiale.
     */
    private String syncEnvironment = "DEV";

    @Data
    public static class EndpointInfo {
        private String wsdlUrl;
        private String endpointUrl;
    }
}

