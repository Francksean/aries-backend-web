package org.example.ariesbackendweb.MWS.dtos;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * DTO pour soumettre un nouveau test MWS depuis l'interface utilisateur.
 * Ce DTO contient toutes les informations nécessaires pour qu'un test
 * puisse être exécuté par l'agent.
 */
@Data
public class MwsTestSubmissionRequest {

    /**
     * UUID du programme MWS à tester.
     * Utilisé pour retrouver les endpoints et valider l'opération.
     */
    private String programId;

    /**
     * Environnement cible du test (DEV, TEST, PROD).
     * Détermine quel endpoint sera utilisé pour l'exécution.
     */
    private String environment;

    /**
     * Nom de l'opération SOAP à tester.
     * Par exemple : "GetBasicData", "UpdateCustomer"
     */
    private String operationName;

    /**
     * Corps de la requête SOAP au format XML.
     * C'est le XML que l'utilisateur a rempli dans l'UI,
     * avec toutes les valeurs de test spécifiques.
     */
    private String requestBody;

    /**
     * Nom d'utilisateur M3 pour l'authentification.
     * Ces credentials seront transmis à l'agent qui les utilisera
     * pour s'authentifier auprès du serveur MWS.
     */
    private String m3Username;

    /**
     * Mot de passe M3 (devrait être chiffré en transit via HTTPS).
     */
    private String m3Password;

    /**
     * Timeout personnalisé pour ce test en millisecondes.
     * Si non spécifié, une valeur par défaut de 60 secondes sera utilisée.
     */
    private Integer timeoutMillis;

    /**
     * Métadonnées additionnelles optionnelles.
     * Peut contenir des informations contextuelles comme un numéro de ticket,
     * un identifiant de campagne de test, etc.
     */
    private Map<String, String> metadata = new HashMap<>();
}


