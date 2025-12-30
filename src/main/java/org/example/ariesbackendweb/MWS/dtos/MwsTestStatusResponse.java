package org.example.ariesbackendweb.MWS.dtos;


import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO représentant l'état complet d'un test à un instant T.
 * Utilisé pour les requêtes de statut et pour afficher l'avancement
 * dans l'interface utilisateur.
 */
@Data
public class MwsTestStatusResponse {

    private String testId;
    private String programName;
    private String operationName;
    private String environment;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long durationMillis;
    private String submittedBy;

    /**
     * Indicateur de progression pour l'UI (0-100).
     * Permet d'afficher une barre de progression approximative.
     */
    private Integer progressPercentage;

    /**
     * Message descriptif du statut actuel.
     * Par exemple : "En attente de l'agent", "Exécution en cours", etc.
     */
    private String statusMessage;
}
