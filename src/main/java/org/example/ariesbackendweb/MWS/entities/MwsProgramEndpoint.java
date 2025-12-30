package org.example.ariesbackendweb.MWS.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Data
@Entity
@Table(name = "mws_program_endpoint")
public class MwsProgramEndpoint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    private MwsProgram program;

    @Column(nullable = false, length = 20)
    private String environment; // 'DEV', 'TEST', 'PROD'

    @Column(name = "wsdl_url", nullable = false, length = 500)
    private String wsdlUrl;

    @Column(name = "endpoint_url", nullable = false, length = 500)
    private String endpointUrl;
}
