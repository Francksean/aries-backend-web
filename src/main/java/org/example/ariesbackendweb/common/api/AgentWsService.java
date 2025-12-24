package org.example.ariesbackendweb.common.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.RestTemplateXhrTransport;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class AgentWsService {

    private static final Logger log = LoggerFactory.getLogger(AgentWsService.class);

    @Autowired
    private SimpMessagingTemplate brokerMessagingTemplate;

    @Autowired
    private ApplicationEventPublisher eventPublisher;



    @Value("${agent.ws.url}")
    private String agentWsUrl;

    private final Map<String, String> sessionIds = new ConcurrentHashMap<>();

    public void connect(String sessionId, String logFileName) throws ExecutionException, InterruptedException, IOException {

        // Configuration des transports (WebSocket + XHR fallback)
        List<Transport> transports = Arrays.asList(
                new WebSocketTransport(new StandardWebSocketClient()),
                new RestTemplateXhrTransport()
        );
        SockJsClient sockJsClient = new SockJsClient(transports);

        WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new StringMessageConverter());

        // Préparation du fichier de logs
        PrintWriter writer = new PrintWriter(logFileName + ".txt", StandardCharsets.UTF_8);

        try {
            stompClient.connectAsync(agentWsUrl, new AgentSessionHandler(sessionId, writer, brokerMessagingTemplate, eventPublisher)).get(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.error("Connection timeout for sessionId={}", sessionId);
            writer.close();
            throw new IOException("Connection timeout", e);
        }

        log.info("Connexion établie pour sessionId={}", sessionId);
    }
}