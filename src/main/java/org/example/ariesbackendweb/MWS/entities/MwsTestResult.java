package org.example.ariesbackendweb.MWS.entities;


import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "mws_test_result")
@Data
@NoArgsConstructor
public class MwsTestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "result_id", nullable = false, unique = true, length = 36)
    private String resultId; // UUID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private MwsTestRequest request;

    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName;

    @Column(name = "operation_name", nullable = false, length = 100)
    private String operationName;

    @Column(nullable = false)
    private Boolean success;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "response_body", columnDefinition = "LONGTEXT")
    private String responseBody; // XML SOAP de réponse

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "exception_stack_trace", columnDefinition = "LONGTEXT")
    private String exceptionStackTrace;

    @Column(name = "start_time", nullable = false)
    private Long startTime; // Timestamp en millisecondes

    @Column(name = "end_time", nullable = false)
    private Long endTime;

    @Column(name = "duration_millis", nullable = false)
    private Long durationMillis;

    @Column(name = "time_taken")
    private Long timeTaken; // Temps réseau uniquement

    @Column(name = "executed_by", length = 100)
    private String executedBy;

    @Column(name = "agent_version", length = 50)
    private String agentVersion;

    @Column(length = 20)
    private String environment;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (resultId == null) {
            resultId = java.util.UUID.randomUUID().toString();
        }
        // Calculer la durée si startTime et endTime sont définis
        if (startTime != null && endTime != null) {
            durationMillis = endTime - startTime;
        }
    }
}
