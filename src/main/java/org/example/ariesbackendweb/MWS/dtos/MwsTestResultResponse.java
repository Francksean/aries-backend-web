package org.example.ariesbackendweb.MWS.dtos;


import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO complet contenant les résultats d'un test terminé.
 * C'est l'objet que l'utilisateur consulte une fois le test fini
 * pour analyser ce qui s'est passé.
 */
@Data
public class MwsTestResultResponse {

    // Informations du test
    private String testId;
    private String programName;
    private String operationName;
    private String environment;
    private String status;

    // Timing
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long durationMillis;
    private Long networkTimeMillis;

    // Requête originale
    private String requestBody;

    // Résultat
    private Boolean success;
    private Integer httpStatus;
    private String responseBody;
    private Map<String, String> responseHeaders;
    private String errorMessage;
    private String exceptionStackTrace;

    // Métadonnées d'exécution
    private String executedBy;
    private String agentVersion;
    private String submittedBy;
}