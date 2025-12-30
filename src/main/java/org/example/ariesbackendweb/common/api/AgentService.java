package org.example.ariesbackendweb.common.api;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ariesbackendweb.MEC.DTOs.LaunchAgentTestDto;
import org.example.ariesbackendweb.MWS.dtos.agent.MwsAgentTestRequestDto;
import org.example.ariesbackendweb.MWS.dtos.agent.MwsAgentTestResult;
import org.example.ariesbackendweb.MWS.entities.*;
import org.example.ariesbackendweb.MWS.repositories.MwsOperationRepository;
import org.example.ariesbackendweb.MWS.repositories.MwsProgramRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Classe pour la communication avec l'agent, exploitera le repository/apiClient
 * */
@Service
@AllArgsConstructor
@Slf4j
public class AgentService {

    @Value("${agent.url}")
    private String agentBaseUrl;

    private RestClient restClient;

    @Autowired
    private MwsProgramRepository programRepository;

    @Autowired
    private MwsOperationRepository operationRepository;

    
    public AgentService (){
        this.restClient = RestClient.builder()
                .baseUrl(agentBaseUrl)
                .build();
    }


    public void launchMecTestAgent(LaunchAgentTestDto data) throws IOException {
        MultiValueMap<String, Object> body = getBody(data);

        Map<?, ?> response = restClient.post()
                .uri(agentBaseUrl + "/mec/launch")
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

    /**
     * Synchronise les opérations d'un programme depuis son WSDL.
     *
     * Cette méthode représente le cœur de notre approche hybride. Contrairement
     * à une synchronisation complète qui analyserait en profondeur tous les paramètres
     * de chaque opération, nous nous contentons ici de récupérer uniquement les noms
     * des opérations et leurs descriptions.
     *
     * Cette approche légère nous donne plusieurs avantages. Premièrement, la synchronisation
     * est très rapide car nous ne parsons pas les schémas XSD complexes. Deuxièmement,
     * elle utilise peu de bande passante réseau. Troisièmement, elle stocke peu de
     * données en base, ce qui réduit les coûts de stockage et simplifie la maintenance.
     *
     * Les détails des paramètres et les templates SOAP seront générés dynamiquement
     * à la demande lorsque l'utilisateur sélectionne une opération à tester.
     */
    @Transactional
    public void syncOperations(UUID programId, String environment) {
        log.info("Synchronisation des opérations pour le programme {} dans l'environnement {}",
                programId, environment);

        MwsProgram program = programRepository.findById(programId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Programme non trouvé avec l'ID: " + programId));

        // Recherche de l'endpoint pour l'environnement spécifié
        MwsProgramEndpoint endpoint = program.getEndpoints().stream()
                .filter(e -> e.getEnvironment().equalsIgnoreCase(environment))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Aucun endpoint configuré pour l'environnement: " + environment));

        String wsdlUrl = endpoint.getWsdlUrl();
        log.debug("URL WSDL pour la synchronisation: {}", wsdlUrl);

        // Construction de l'URL pour appeler l'agent
        // Nous utilisons UriComponentsBuilder pour gérer correctement l'encodage des paramètres

        String[] operationNames;
        try {
            operationNames = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/wsdl/operations")
                            .queryParam("wsdlUrl", wsdlUrl)
                            .build())
                    .retrieve()
                    .body(String[].class);
        } catch (RestClientException e) {
            log.error("Erreur lors de l'appel à l'agent pour la découverte des opérations", e);
            throw new RuntimeException(
                    "Impossible de contacter l'agent MWS. Vérifiez que l'agent est démarré et accessible. " +
                            "Erreur: " + e.getMessage(), e);
        }


        if (operationNames == null || operationNames.length == 0) {
            log.warn("Aucune opération trouvée dans le WSDL");
            throw new IllegalStateException(
                    "Le WSDL ne contient aucune opération. Vérifiez que l'URL WSDL est correcte.");
        }

        log.info("{} opération(s) découverte(s) dans le WSDL", operationNames.length);

        // Suppression des anciennes opérations
        // Dans une approche plus sophistiquée, nous pourrions faire un merge intelligent
        // pour détecter les opérations ajoutées, supprimées et inchangées. Mais pour
        // la simplicité et la clarté, nous supprimons tout et recréons.
        //TODO : penser à un merge plus intelligent.
        int deletedCount = operationRepository.deleteByProgram(program);
        log.debug("{} ancienne(s) opération(s) supprimée(s)", deletedCount);

        // Création des nouvelles opérations
        for (String operationName : operationNames) {
            MwsOperation operation = new MwsOperation();
            operation.setProgram(program);
            operation.setOperationName(operationName);
            // La description et le template seront null car nous ne les stockons pas
            // Ils seront générés dynamiquement à la demande

            operationRepository.save(operation);

            log.debug("Opération créée: {}", operationName);
        }

        // Mise à jour de la date de dernière synchronisation
        program.setLastSyncedAt(LocalDateTime.now());
        programRepository.save(program);

        log.info("Synchronisation terminée avec succès. {} opération(s) enregistrée(s)",
                operationNames.length);
    }


    /**
     * Récupère le template SOAP pour une opération spécifique.
     *
     * Cette méthode est au cœur de notre approche dynamique. Au lieu de retourner
     * un template pré-stocké en base de données, nous demandons à l'agent de le
     * générer en temps réel depuis le WSDL. Cela garantit que le template est
     * toujours à jour avec la version actuelle du service MWS.
     *
     * Nous utilisons une annotation @Cacheable pour mettre en cache le résultat
     * pendant quelques minutes. Cela évite de bombarder l'agent de requêtes
     * identiques tout en conservant une fraîcheur raisonnable des données.
     */
    @Cacheable(value = "soapTemplates", key = "#programId + '-' + #environment + '-' + #operationName")
    public String getOperationTemplate(UUID programId, String environment, String operationName) {
        log.info("Récupération du template SOAP pour {}/{}/{}",
                programId, environment, operationName);

        MwsProgram program = programRepository.findById(programId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Programme non trouvé avec l'ID: " + programId));

        // Vérification que l'opération existe bien pour ce programme
        // Cela évite des appels inutiles à l'agent pour des opérations inexistantes
        boolean operationExists = program.getOperations().stream()
                .anyMatch(op -> op.getOperationName().equalsIgnoreCase(operationName));

        if (!operationExists) {
            log.warn("Opération '{}' non trouvée pour le programme '{}'",
                    operationName, program.getCode());
            throw new IllegalArgumentException(
                    "L'opération '" + operationName + "' n'existe pas pour ce programme. " +
                            "Vérifiez le nom ou lancez une synchronisation si l'opération a été ajoutée récemment.");
        }

        // Recherche de l'endpoint pour l'environnement
        MwsProgramEndpoint endpoint = program.getEndpoints().stream()
                .filter(e -> e.getEnvironment().equalsIgnoreCase(environment))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Aucun endpoint configuré pour l'environnement: " + environment));

        String wsdlUrl = endpoint.getWsdlUrl();

        // Construction de l'URL pour appeler l'agent
        String template;
        try {
            template = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/wsdl/template")
                            .queryParam("wsdlUrl", wsdlUrl)
                            .queryParam("operationName", operationName)
                            .build())
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            log.error("Erreur lors de l'appel à l'agent pour la génération du template", e);
            throw new RuntimeException(
                    "Impossible de générer le template SOAP. Vérifiez que l'agent est accessible. " +
                            "Erreur: " + e.getMessage(), e);
        }


        if (template == null || template.trim().isEmpty()) {
            log.error("L'agent a retourné un template vide pour {}", operationName);
            throw new IllegalStateException(
                    "Le template généré est vide. Il y a peut-être un problème avec le WSDL.");
        }

        log.debug("Template généré avec succès: {} caractères", template.length());

        return template;
    }

    /**
     * Envoie une demande de test à l'agent pour exécution.
     *
     * Cette méthode transforme les données de notre modèle interne
     * en un DTO que l'agent comprend, puis effectue l'appel HTTP.
     * Elle gère aussi les erreurs de communication réseau de manière
     * robuste en les transformant en exceptions métier compréhensibles.
     *
     * @param testRequest La demande de test à envoyer
     * @return Le résultat du test retourné par l'agent
     * @throws AgentCommunicationException Si la communication avec l'agent échoue
     */
    public MwsAgentTestResult sendTestToAgent(MwsAgentTestRequestDto testRequest) {
        log.info("Envoi du test {} à l'agent: {}",
                testRequest.getSessionId(),
                agentBaseUrl);

        // Construction de l'URL complète de l'endpoint de l'agent
        String url = agentBaseUrl + "/mws/launch";


        try {
            // Appel HTTP POST vers l'agent
            // Nous utilisons exchange() plutôt que postForObject() pour avoir
            // plus de contrôle sur les headers et pouvoir récupérer le code HTTP
            MwsAgentTestResult result = restClient.post()
                    .uri("/api/test/execute")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(testRequest)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        String errorBody = extractErrorBody(response);
                        log.error("L'agent a retourné une erreur pour le test {}: Status={}, Body={}",
                                testRequest.getSessionId(),
                                response.getStatusCode(),
                                errorBody);

                        throw new AgentCommunicationException(
                                "Erreur de l'agent MWS (Status: " + response.getStatusCode() + "): " + errorBody
                        );
                    })
                    .body(MwsAgentTestResult.class);

            // Vérification que nous avons bien reçu une réponse
            if (result == null) {
                log.error("L'agent a retourné une réponse vide pour le test {}",
                        testRequest.getSessionId());
                throw new AgentCommunicationException(
                        "L'agent a retourné une réponse vide. Vérifiez les logs de l'agent.");
            }

            log.info("Test {} exécuté par l'agent avec succès: {}",
                    testRequest.getSessionId(),
                    result.isSuccess() ? "SUCCESS" : "FAILED");

            return result;

        } catch (RestClientException e) {
            // Cette exception est levée par RestTemplate en cas de problème réseau,
            // timeout, erreur HTTP 4xx ou 5xx, etc.
            log.error("Erreur de communication avec l'agent pour le test {}",
                    testRequest.getSessionId(), e);

            // Nous transformons l'exception technique en exception métier
            // avec un message plus compréhensible pour l'utilisateur
            throw new AgentCommunicationException(
                    "Impossible de communiquer avec l'agent MWS. " +
                            "Vérifiez que l'agent est démarré et accessible. " +
                            "Erreur: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Vérifie si l'agent est accessible et opérationnel.
     *
     * Cette méthode peut être utilisée avant d'envoyer un test pour
     * détecter rapidement si l'agent est down, évitant ainsi de perdre
     * du temps à attendre un timeout.
     *
     * @return true si l'agent répond, false sinon
     */
    public boolean isAgentAvailable() {
        log.debug("Vérification de la disponibilité de l'agent: {}", agentBaseUrl);

        try {
            // Tentative de ping vers l'agent
            // Vous devriez avoir un endpoint /health ou /ping dans votre agent
            return restClient.get()
                    .uri("/actuator/health")
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        log.warn("Health check de l'agent a retourné un statut d'erreur: {}",
                                response.getStatusCode());
                    })
                    .toBodilessEntity()
                    .getStatusCode() == HttpStatus.OK;

        } catch (RestClientException e) {
            log.warn("Agent non disponible: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Exception personnalisée pour les erreurs de communication avec l'agent.
     * Cette exception permet de distinguer les erreurs réseau des erreurs
     * métier et de les traiter différemment dans votre code.
     */
    public static class AgentCommunicationException extends RuntimeException {
        public AgentCommunicationException(String message) {
            super(message);
        }

        public AgentCommunicationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private String extractErrorBody(ClientHttpResponse response) {
        try {
            return new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Impossible de lire le corps de l'erreur", e);
            return "Impossible de lire le message d'erreur";
        }
    }
}
