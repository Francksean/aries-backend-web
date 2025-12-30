package org.example.ariesbackendweb.common.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;

import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.util.Map;

@Slf4j
public class AgentSessionHandler implements StompSessionHandler {

    private final String sessionId;
    private final PrintWriter writer;
    private final SimpMessagingTemplate brokerMessagingTemplate;
    private final ApplicationEventPublisher eventPublisher;

    public AgentSessionHandler(String sessionId, PrintWriter writer, SimpMessagingTemplate brokerMessagingTemplate, ApplicationEventPublisher eventPublisher) {
        this.sessionId = sessionId;
        this.writer = writer;
        this.brokerMessagingTemplate = brokerMessagingTemplate;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
        // Souscription aux topics
        subscribeToTopics(session);
    }

    private void subscribeToTopics(StompSession session) {
        // TODO : faire passer ça en config
        // Souscription aux logs
        String logsDestination = "/topic/logs/" + sessionId;
        log.info("Souscription à: {}", logsDestination);
        session.subscribe(logsDestination, this);

        // Souscription aux status
        String statusDestination = "/topic/status/" + sessionId;
        log.info("Souscription à: {}", statusDestination);
        session.subscribe(statusDestination, this);

        // Souscription aux fichiers
        String fileDestination = "/topic/file/" + sessionId;
        log.info("Souscription à: {}", fileDestination);
        session.subscribe(fileDestination, this);

        log.info("Toutes les souscriptions actives pour sessionId={}", sessionId);
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
        log.info("MESSAGE REÇU");

        if (payload == null) {
            log.warn("Payload null reçu");
            return;
        }

        String message = payload.toString();
        log.info("   - Message: {}", message);

        // Déterminer le type de message selon la destination
        String destination = headers.getDestination();
        if (destination != null) {
            if (destination.contains("/logs/")) {
                handleLogs(message, sessionId, writer);
            } else if (destination.contains("/status/")) {
                handleStatus(message, sessionId);
            }
        }
    }

    @Override
    public void handleException(StompSession session, StompCommand command,
                                StompHeaders headers, byte[] payload, Throwable exception) {
        log.error("STOMP EXCEPTION for sessionId={}", sessionId, exception);
    }

    @Override
    public void handleTransportError(StompSession session, Throwable exception) {
        log.error("TRANSPORT ERROR for sessionId={}", sessionId, exception);

        // Reconnexion automatique si déconnecté
        if (!session.isConnected()) {
            log.warn("Session déconnectée, tentative de reconnexion...");
            // TODO: Implémenter la logique de reconnexion
        }
    }

    @Override
    public Type getPayloadType(StompHeaders headers) {
        // Important : retourner String.class pour recevoir les messages en texte
        return String.class;
    }

    /**
     * Gestion des messages de status
     */
    private void handleStatus(String message, String sessionId) {
        log.info("STATUS AGENT [{}] -> {}", sessionId, message);
        try {
            brokerMessagingTemplate.convertAndSend("/topic/status/" + sessionId,
                    Map.of("status", message, "timestamp", System.currentTimeMillis()).toString());
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du status", e);
        }
    }

    /**
     * Réception et traitement des logs
     */
    private void handleLogs(String message, String sessionId, PrintWriter writer) {
        log.info("LOG AGENT [{}] -> {}", sessionId, message);
        try {
            writer.println(message);
            writer.flush();

            brokerMessagingTemplate.convertAndSend("/topic/logs/" + sessionId,
                    Map.of("log", message, "timestamp", System.currentTimeMillis()).toString());
        } catch (Exception e) {
            log.error("Erreur lors du traitement du log", e);
        }
    }


}
