package org.example.ariesbackendweb.MWS.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "mws_operations")
@Data
public class MwsOperation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private MwsProgram program;

    @Column(name = "operation_name", nullable = false, length = 100)
    private String operationName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "operation")
    private Set<MwsTestRequest> testRequests = new HashSet<>();
}
