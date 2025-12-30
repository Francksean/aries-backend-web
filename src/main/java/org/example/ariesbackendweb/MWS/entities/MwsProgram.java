package org.example.ariesbackendweb.MWS.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.ariesbackendweb.common.entities.Program;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "mws_programs")
@Data
public class MwsProgram extends Program {
    @Column
    private String description;

    @Column(name = "program_type", nullable = false, length = 20)
    private String programType; // 'API', 'MDP', ou 'SQL'

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    // Relations
    @OneToMany(mappedBy = "program", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MwsProgramEndpoint> endpoints = new HashSet<>();

    @OneToMany(mappedBy = "program", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MwsOperation> operations = new HashSet<>();

    @OneToMany(mappedBy = "program", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MwsProgramTag> tags = new HashSet<>();

    @OneToMany(mappedBy = "program")
    private Set<MwsTestRequest> testRequests = new HashSet<>();
}
