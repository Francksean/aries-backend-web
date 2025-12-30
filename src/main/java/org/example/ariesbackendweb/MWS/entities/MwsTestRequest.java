package org.example.ariesbackendweb.MWS.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.ariesbackendweb.common.entities.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Table(name = "mec_program_test")
@Entity
@Data
public class MwsTestRequest extends Test {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id")
    private MwsProgram program;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operation_id", insertable = false, updatable = false)
    private MwsOperation operation;

    @Enumerated(EnumType.STRING)
    private MwsTestStatus status;

    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName;

    @Column(name = "operation_name", nullable = false, length = 100)
    private String operationName;

    @Column(name = "wsdl_url", nullable = false, length = 500)
    private String wsdlUrl;

    @Column(name = "endpoint_url", nullable = false, length = 500)
    private String endpointUrl;

    @Column(name = "request_body", columnDefinition = "LONGTEXT", nullable = false)
    private String requestBody; // XML SOAP

    @Column(length = 100)
    private String M3Username; // Credentials M3

    @Column(name = "password_encrypted", length = 500)
    private String M3Password; // Mot de passe chiffré

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "timeout_millis")
    private Integer timeoutMillis = 60000;

    @Column(nullable = false, length = 20)
    private String environment;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MwsTestResult> results = new HashSet<>();


}
