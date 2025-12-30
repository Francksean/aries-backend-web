package org.example.ariesbackendweb.MWS.dtos;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO de réponse retourné immédiatement après la soumission d'un test.
 * L'utilisateur reçoit cet objet qui lui permet de suivre l'avancement
 * de son test via son identifiant unique.
 */
@Data
public class MwsTestSubmissionResponse {

    /**
     * Identifiant unique du test qui vient d'être soumis.
     * Cet UUID sera utilisé pour toutes les opérations futures
     * sur ce test : récupération du statut, des résultats, annulation, etc.
     */
    private String testId;

    /**
     * Statut initial du test au moment de la soumission.
     * Généralement "CREATED" juste après la création.
     */
    private String status;

    /**
     * Timestamp de création du test.
     */
    private LocalDateTime createdAt;

    /**
     * Message de confirmation pour l'utilisateur.
     */
    private String message;

    /**
     * Constructeur de convenance pour créer rapidement une réponse.
     */
    public MwsTestSubmissionResponse(String testId, String status, String message) {
        this.testId = testId;
        this.status = status;
        this.message = message;
        this.createdAt = LocalDateTime.now();
    }
}
