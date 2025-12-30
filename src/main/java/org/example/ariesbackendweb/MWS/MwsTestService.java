package org.example.ariesbackendweb.MWS;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.example.ariesbackendweb.MWS.dtos.MwsTestResultResponse;
import org.example.ariesbackendweb.MWS.dtos.MwsTestStatusResponse;
import org.example.ariesbackendweb.MWS.dtos.MwsTestSubmissionRequest;
import org.example.ariesbackendweb.MWS.dtos.MwsTestSubmissionResponse;
import org.example.ariesbackendweb.MWS.dtos.agent.MwsAgentTestRequestDto;
import org.example.ariesbackendweb.MWS.dtos.agent.MwsAgentTestResult;
import org.example.ariesbackendweb.MWS.entities.*;
import org.example.ariesbackendweb.MWS.repositories.MwsProgramRepository;
import org.example.ariesbackendweb.MWS.repositories.MwsTestRepository;
import org.example.ariesbackendweb.MWS.repositories.MwsTestResultRepository;
import org.example.ariesbackendweb.common.api.AgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MwsTestService {
    @Autowired
    MwsTestRepository testRepository;

    @Autowired
    private MwsTestRepository testRequestRepository;

    @Autowired
    private MwsTestResultRepository testResultRepository;

    @Autowired
    private MwsProgramRepository programRepository;

    @Autowired
    private AgentService agentService;

    @Autowired
    private SimpMessagingTemplate brokerMessagingTemplate;


    /**
     * Soumet un nouveau test pour exécution.
     * <p>
     * Cette méthode est le point d'entrée principal pour lancer un test.
     * Elle effectue toutes les validations nécessaires, crée l'entité de test
     * en base de données, et déclenche l'exécution asynchrone.
     * <p>
     * Le principe clé ici est de retourner immédiatement à l'utilisateur
     * sans attendre la fin du test. L'utilisateur reçoit un UUID qui lui
     * permet de suivre l'avancement de son test.
     *
     * @param request Les paramètres du test à lancer
     * @return Une réponse contenant l'UUID du test créé
     * @throws IllegalArgumentException Si les données sont invalides
     */
    @Transactional
    public MwsTestSubmissionResponse submitTest(MwsTestSubmissionRequest request) {
        log.info("Soumission d'un nouveau test pour le programme {} / {}",
                request.getProgramId(), request.getOperationName());

        // Étape 1 : Validation du programme MWS
        UUID programUuid = UUID.fromString(request.getProgramId());
        MwsProgram program = programRepository.findById(programUuid)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Programme MWS non trouvé: " + request.getProgramId()));

        if (!program.isActive()) {
            throw new IllegalArgumentException(
                    "Le programme " + program.getCode() + " est désactivé");
        }

        // Étape 2 : Validation de l'environnement et récupération de l'endpoint
        MwsProgramEndpoint endpoint = program.getEndpoints().stream()
                .filter(e -> e.getEnvironment().equalsIgnoreCase(request.getEnvironment()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Aucun endpoint configuré pour l'environnement: " + request.getEnvironment()));

        // Étape 3 : Validation de l'opération
        boolean operationExists = program.getOperations().stream()
                .anyMatch(op -> op.getOperationName().equalsIgnoreCase(request.getOperationName()));

        if (!operationExists) {
            throw new IllegalArgumentException(
                    "L'opération '" + request.getOperationName() +
                            "' n'existe pas pour ce programme. Lancez une synchronisation si nécessaire.");
        }

        // Étape 4 : Récupération de l'utilisateur connecté
        // En production, vous récupéreriez cela depuis Spring Security
//        String currentUser = getCurrentUsername();

        // Étape 5 : Création de l'entité MwsTestRequest
        MwsTestRequest testRequest = new MwsTestRequest();
        testRequest.setProgram(program);
        testRequest.setServiceName(program.getCode());
        testRequest.setOperationName(request.getOperationName());
        testRequest.setWsdlUrl(endpoint.getWsdlUrl());
        testRequest.setEndpointUrl(endpoint.getEndpointUrl());
        testRequest.setRequestBody(request.getRequestBody());
        testRequest.setM3Username(request.getM3Username());
        testRequest.setM3Password(request.getM3Password()); // À chiffrer en production
        testRequest.setTimeoutMillis(request.getTimeoutMillis() != null ?
                request.getTimeoutMillis() : 60000);
        testRequest.setEnvironment(request.getEnvironment());
        // TODO régler ça et permettre l'insertion du UUID user
//        testRequest.setLaunchedBy(currentUser);
        testRequest.setStatus(MwsTestStatus.CREATED);
        testRequest.setCreatedAt(LocalDateTime.now());

        // Sauvegarde pour générer l'UUID
        testRequest = testRequestRepository.save(testRequest);

        log.info("Test créé avec l'ID: {}", testRequest.getId());

        // Étape 6 : Déclenchement de l'exécution asynchrone
        // L'annotation @Async sur la méthode executeTestAsync fait que
        // cette méthode s'exécute dans un thread séparé, permettant à
        // submitTest() de retourner immédiatement
        executeTestAsync(testRequest.getId());

        // Étape 7 : Construction de la réponse
        return new MwsTestSubmissionResponse(
                testRequest.getId().toString(),
                testRequest.getStatus().name(),
                "Test soumis avec succès. Vous serez notifié une fois l'exécution terminée."
        );
    }

    /**
     * Exécute un test de manière asynchrone.
     * <p>
     * Cette méthode s'exécute dans un thread séparé grâce à @Async.
     * Elle gère toute l'interaction avec l'agent et la persistance
     * des résultats, tout en notifiant l'utilisateur des changements
     * de statut via WebSocket.
     * <p>
     * L'annotation @Async nécessite que votre application Spring Boot
     * ait activé le support asynchrone via @EnableAsync dans une classe
     * de configuration.
     *
     * @param testId L'UUID du test à exécuter
     */
    @Async
    public void executeTestAsync(UUID testId) {
        log.info("Début de l'exécution asynchrone du test: {}", testId);

        MwsTestRequest testRequest = null;

        try {
            // Récupération du test depuis la base de données
            // Nous utilisons une nouvelle transaction car nous sommes dans un thread séparé
            testRequest = testRequestRepository.findById(testId)
                    .orElseThrow(() -> new RuntimeException("Test non trouvé: " + testId));

            // Mise à jour du statut : en attente de l'agent
            updateTestStatus(testRequest, MwsTestStatus.PENDING_AGENT,
                    "Envoi du test à l'agent...");

            // Construction du DTO pour l'agent
            MwsAgentTestRequestDto agentRequest = buildAgentRequest(testRequest);

            // Appel à l'agent pour exécuter le test
            // Cet appel est synchrone du point de vue de ce thread, mais asynchrone
            // du point de vue de l'utilisateur qui a déjà reçu sa réponse HTTP
            log.info("Envoi du test {} à l'agent", testId);
            updateTestStatus(testRequest, MwsTestStatus.RUNNING,
                    "Test en cours d'exécution...");

            MwsAgentTestResult agentResult = agentService.sendTestToAgent(agentRequest);

            // Traitement du résultat retourné par l'agent
            processTestResult(testRequest, agentResult);


        } catch (Exception e) {
            // Toute autre erreur inattendue
            log.error("Erreur inattendue lors de l'exécution du test {}", testId, e);

            if (testRequest != null) {
                markTestAsFailed(testRequest,
                        "Erreur système: " + e.getMessage());
            }
        }
    }

    /**
     * Traite le résultat reçu de l'agent et le persiste en base de données.
     * <p>
     * Cette méthode crée une nouvelle entité MwsTestResult, met à jour
     * le statut de la demande de test, et notifie l'utilisateur que
     * les résultats sont disponibles.
     */
    @Transactional
    protected void processTestResult(MwsTestRequest testRequest, MwsAgentTestResult agentResult) {
        log.info("Traitement du résultat du test: {}", testRequest.getId());

        // Création de l'entité résultat
        MwsTestResult result = new MwsTestResult();
        result.setRequest(testRequest);
        result.setResultId(UUID.randomUUID().toString());
        result.setServiceName(testRequest.getServiceName());
        result.setOperationName(testRequest.getOperationName());
        result.setSuccess(agentResult.isSuccess());
        result.setHttpStatus(agentResult.getHttpStatus());
        result.setResponseBody(agentResult.getResponseBody());
        result.setErrorMessage(agentResult.getErrorMessage());
        result.setExceptionStackTrace(agentResult.getExceptionStackTrace());
        result.setStartTime(agentResult.getStartTime());
        result.setEndTime(agentResult.getEndTime());
        result.setDurationMillis(agentResult.getDurationMillis());
        result.setTimeTaken(agentResult.getTimeTaken());
        result.setExecutedBy(agentResult.getExecutedBy());
        result.setAgentVersion(agentResult.getAgentVersion());
        result.setEnvironment(testRequest.getEnvironment());

        // Sauvegarde du résultat
        testResultRepository.save(result);

        // Mise à jour du statut de la demande
        MwsTestStatus finalStatus = agentResult.isSuccess() ?
                MwsTestStatus.SUCCESS : MwsTestStatus.FAILED;

        testRequest.setStatus(finalStatus);
        // TODO : ajouter le completedAt
        testRequest.setCompletedAt(LocalDateTime.now());
        testRequestRepository.save(testRequest);

        // Notification de l'utilisateur
        brokerMessagingTemplate.convertAndSend("/topic/status/" + testRequest.getId(),
                MwsTestStatus.COMPLETED.name());

        log.info("Test {} terminé avec le statut: {}",
                testRequest.getId(), finalStatus);
    }

    /**
     * Marque un test comme ayant échoué suite à une erreur.
     */
    @Transactional
    protected void markTestAsFailed(MwsTestRequest testRequest, String errorMessage) {
        log.warn("Marquage du test {} comme FAILED: {}",
                testRequest.getId(), errorMessage);

        // Création d'un résultat d'échec
        MwsTestResult result = new MwsTestResult();
        result.setRequest(testRequest);
        result.setResultId(UUID.randomUUID().toString());
        result.setServiceName(testRequest.getServiceName());
        result.setOperationName(testRequest.getOperationName());
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        result.setStartTime(System.currentTimeMillis());
        result.setEndTime(System.currentTimeMillis());
        result.setDurationMillis(0L);
        result.setEnvironment(testRequest.getEnvironment());

        testResultRepository.save(result);

        // Mise à jour du statut
        testRequest.setStatus(MwsTestStatus.FAILED);
        testRequest.setCompletedAt(LocalDateTime.now());
        testRequestRepository.save(testRequest);

        // Notification
        brokerMessagingTemplate.convertAndSend("/topic/status/" + testRequest.getId(),
                MwsTestStatus.FAILED.name());
    }

    /**
     * Met à jour le statut d'un test et notifie l'utilisateur.
     */
    @Transactional
    protected void updateTestStatus(MwsTestRequest testRequest,
                                    MwsTestStatus newStatus,
                                    String message) {
        log.debug("Mise à jour statut test {} : {} -> {}",
                testRequest.getId(), testRequest.getStatus(), newStatus);

        testRequest.setStatus(newStatus);
        testRequestRepository.save(testRequest);

        // Notification temps réel
        brokerMessagingTemplate.convertAndSend("/topic/status/" + testRequest.getId(),
                newStatus.name());
    }

    /**
     * Construit le DTO de requête pour l'agent à partir de notre entité.
     */
    private MwsAgentTestRequestDto buildAgentRequest(MwsTestRequest testRequest) {
        MwsAgentTestRequestDto dto = new MwsAgentTestRequestDto();
        dto.setSessionId(testRequest.getId().toString());
        dto.setServiceName(testRequest.getServiceName());
        dto.setWsdlUrl(testRequest.getWsdlUrl());
        dto.setEndpointUrl(testRequest.getEndpointUrl());
        dto.setOperationName(testRequest.getOperationName());
        dto.setRequestBody(testRequest.getRequestBody());
        dto.setUsername(testRequest.getM3Username());
        dto.setPassword(testRequest.getM3Password());
        dto.setTimeoutMillis(testRequest.getTimeoutMillis());
        dto.setCreatedAt(testRequest.getCreatedAt());
//        dto.setSubmittedBy(testRequest.getLaunchedBy());
        dto.setEnvironment(testRequest.getEnvironment());

        return dto;
    }

    /**
     * Récupère le statut actuel d'un test.
     * <p>
     * Cette méthode est appelée par l'UI pour afficher l'avancement d'un test.
     * Elle peut être pollée régulièrement ou appelée suite à une notification
     * WebSocket pour récupérer les détails.
     */
    @Transactional
    public MwsTestStatusResponse getTestStatus(UUID testId) {
        log.debug("Récupération du statut du test: {}", testId);

        MwsTestRequest testRequest = testRequestRepository.findById(testId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Test non trouvé: " + testId));

        MwsTestStatusResponse response = new MwsTestStatusResponse();
        response.setTestId(testId.toString());
        response.setProgramName(testRequest.getServiceName());
        response.setOperationName(testRequest.getOperationName());
        response.setEnvironment(testRequest.getEnvironment());
        response.setStatus(testRequest.getStatus().name());
        response.setCreatedAt(testRequest.getCreatedAt());
        response.setCompletedAt(testRequest.getCompletedAt());
//        response.setSubmittedBy(testRequest.getSubmittedBy());

        // Calcul de la durée si le test est terminé
        if (testRequest.getCompletedAt() != null) {
            response.setDurationMillis(
                    java.time.Duration.between(
                            testRequest.getCreatedAt(),
                            testRequest.getCompletedAt()
                    ).toMillis()
            );
        }

        // Calcul du pourcentage de progression approximatif
        response.setProgressPercentage(calculateProgress(testRequest.getStatus()));
        response.setStatusMessage(getStatusMessage(testRequest.getStatus()));

        return response;
    }

    /**
     * Récupère les résultats complets d'un test terminé.
     */
    @Transactional
    public MwsTestResultResponse getTestResult(UUID testId) {
        log.info("Récupération des résultats du test: {}", testId);

        MwsTestRequest testRequest = testRequestRepository.findById(testId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Test non trouvé: " + testId));

        // Vérification que le test est terminé
        if (testRequest.getStatus() != MwsTestStatus.SUCCESS &&
                testRequest.getStatus() != MwsTestStatus.FAILED) {
            throw new IllegalStateException(
                    "Le test n'est pas encore terminé. Statut actuel: " + testRequest.getStatus());
        }

        // Récupération du résultat
        MwsTestResult result = testRequest.getResults().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Aucun résultat trouvé pour ce test"));

        // Construction de la réponse
        MwsTestResultResponse response = new MwsTestResultResponse();
        response.setTestId(testId.toString());
        response.setProgramName(testRequest.getServiceName());
        response.setOperationName(testRequest.getOperationName());
        response.setEnvironment(testRequest.getEnvironment());
        response.setStatus(testRequest.getStatus().name());
        response.setCreatedAt(testRequest.getCreatedAt());
        response.setCompletedAt(testRequest.getCompletedAt());
        response.setRequestBody(testRequest.getRequestBody());
        response.setSuccess(result.getSuccess());
        response.setHttpStatus(result.getHttpStatus());
        response.setResponseBody(result.getResponseBody());
//        response.setResponseHeaders(result.getResponseHeaders());
        response.setErrorMessage(result.getErrorMessage());
        response.setExceptionStackTrace(result.getExceptionStackTrace());
        response.setDurationMillis(result.getDurationMillis());
        response.setNetworkTimeMillis(result.getTimeTaken());
        response.setExecutedBy(result.getExecutedBy());
        response.setAgentVersion(result.getAgentVersion());
//        response.setSubmittedBy(testRequest.getSubmittedBy());

        return response;
    }

    /**
     * Liste tous les tests d'un utilisateur.
     */
    // TODO : gérer ça
//    @Transactional
//    public List<MwsTestStatusResponse> listUserTests(String username) {
//        log.debug("Récupération des tests de l'utilisateur: {}", username);
//
//        List<MwsTestRequest> tests = testRequestRepository.findBySubmittedByOrderByCreatedAtDesc(username);
//
//        return tests.stream()
//                .map(test -> {
//                    MwsTestStatusResponse response = new MwsTestStatusResponse();
//                    response.setTestId(test.getId().toString());
//                    response.setProgramName(test.getServiceName());
//                    response.setOperationName(test.getOperationName());
//                    response.setEnvironment(test.getEnvironment());
//                    response.setStatus(test.getStatus().name());
//                    response.setCreatedAt(test.getCreatedAt());
//                    response.setCompletedAt(test.getCompletedAt());
////                    response.setSubmittedBy(test.getSubmittedBy());
//                    return response;
//                })
//                .collect(Collectors.toList());
//    }

    /**
     * Calcule un pourcentage de progression approximatif basé sur le statut.
     */
    private Integer calculateProgress(MwsTestStatus status) {
        return switch (status) {
            case CREATED -> 10;
            case PENDING_AGENT -> 25;
            case RUNNING -> 50;
            case SUCCESS, FAILED -> 100;
            default -> 0;
        };
    }

    /**
     * Retourne un message descriptif pour chaque statut.
     */
    private String getStatusMessage(MwsTestStatus status) {
        return switch (status) {
            case CREATED -> "Test créé, en attente de traitement";
            case PENDING_AGENT -> "Envoi à l'agent en cours...";
            case RUNNING -> "Exécution du test en cours...";
            case SUCCESS -> "Test terminé avec succès";
            case FAILED -> "Test échoué";
            default -> "Statut inconnu";
        };
    }

    /**
     * Récupère l'utilisateur connecté depuis le contexte Spring Security.
     * En développement, retourne un utilisateur par défaut.
     */
    private String getCurrentUsername() {

        // En développement sans authentification
        return "dev-user";

    }

}
