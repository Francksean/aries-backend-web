package org.example.ariesbackendweb.MWS.dtos.agent;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Représente le résultat d'un test MWS exécuté par l'agent.
 * Cette classe contient toutes les informations collectées pendant l'exécution:
 * la réponse SOAP, les métriques de performance, et les éventuelles erreurs.
 *
 * Cycle de vie:
 * 1. Créée par l'agent au début de l'exécution du test
 * 2. Remplie progressivement pendant et après l'exécution
 * 3. Retournée à l'orchestrateur
 * 4. Stockée en base de données par l'orchestrateur
 * 5. Affichée à l'utilisateur dans l'UI
 */

@Data
public class MwsAgentTestResult {

    /**
     * Identifiant unique du résultat.
     * Correspond généralement au requestId de la demande associée.
     */
    private String resultId;

    /**
     * Identifiant de la demande de test associée.
     * Permet de faire le lien entre la demande et son résultat.
     */
    private String requestId;

    /**
     * Nom du service testé.
     * Répété ici pour faciliter les requêtes et l'affichage.
     */
    private String serviceName;

    /**
     * Nom de l'opération testée.
     */
    private String operationName;

    /**
     * Indique si le test a réussi ou échoué.
     * Un test est considéré réussi si:
     * - Le code HTTP de réponse est 200
     * - Il n'y a pas de SOAP Fault dans la réponse
     * - Aucune exception n'a été levée pendant l'exécution
     */
    private boolean success;

    /**
     * Code de statut HTTP de la réponse.
     * Par exemple: 200 (OK), 500 (Internal Server Error), 401 (Unauthorized).
     */
    private int httpStatus;

    /**
     * Corps de la réponse SOAP au format XML.
     * C'est le XML complet retourné par le service MWS.
     *
     * Exemple:
     * <soap:Envelope>
     *   <soap:Body>
     *     <GetBasicDataResponse>
     *       <CUNM>Acme Corporation</CUNM>
     *       <STAT>20</STAT>
     *     </GetBasicDataResponse>
     *   </soap:Body>
     * </soap:Envelope>
     */
    private String responseBody;

    /**
     * En-têtes HTTP de la réponse.
     * Contient des informations utiles pour le débogage comme Content-Type,
     * Content-Length, Server, etc.
     */
    private Map<String, String> responseHeaders = new HashMap<>();

    /**
     * Message d'erreur si le test a échoué.
     * Peut contenir:
     * - Le message d'une SOAP Fault
     * - Le message d'une exception Java
     * - Un message d'erreur personnalisé de l'agent
     */
    private String errorMessage;

    /**
     * Stack trace complète en cas d'exception Java.
     * Très utile pour le débogage mais peut être volumineuse,
     * donc elle n'est pas toujours affichée à l'utilisateur dans l'UI.
     */
    private String exceptionStackTrace;

    /**
     * Timestamp du début de l'exécution du test (en millisecondes depuis epoch).
     */
    private long startTime;

    /**
     * Timestamp de la fin de l'exécution du test (en millisecondes depuis epoch).
     */
    private long endTime;

    /**
     * Durée totale d'exécution en millisecondes.
     * Calculée comme: endTime - startTime.
     * Inclut tout le temps passé, y compris la préparation de la requête,
     * l'envoi, l'attente de la réponse, et le parsing.
     */
    private long durationMillis;

    /**
     * Temps pris par la requête HTTP uniquement (sans la préparation ni le parsing).
     * Cette métrique est fournie par SoapUI ou le client HTTP.
     * Elle représente le temps réel de communication réseau.
     */
    private long timeTaken;

    /**
     * Timestamp de création du résultat (quand l'agent a commencé à traiter le test).
     */
    private LocalDateTime createdAt;

    /**
     * Identifiant de l'agent qui a exécuté le test.
     * Permet de savoir quel serveur/instance d'agent a traité la demande.
     * Utile pour le débogage et la répartition de charge.
     */
    private String executedBy;

    /**
     * Version de l'agent qui a exécuté le test.
     * Permet de corréler des problèmes avec des versions spécifiques de l'agent.
     */
    private String agentVersion;

    /**
     * Environnement d'exécution (DEV, TEST, PROD).
     */
    private String environment;


}
