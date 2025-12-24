package org.example.ariesbackendweb.common.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.ariesbackendweb.common.enums.TestStatus;

import java.sql.Timestamp;
import java.util.UUID;

@Data
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Entity
public class Test {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "launched_by")
    private UUID launchedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="program_id")
    private Program program;

    @Column
    private int duration;

    @Column
    private TestStatus status;

    @Column(name = "launchedOn")
    private Timestamp launchedOn;
}
