package org.example.ariesbackendweb.MEC.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.example.ariesbackendweb.common.entities.Program;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "mec_programs")
@Entity
public class MecProgram extends Program {

    @Column(name = "deposit_host")
    private String depositHost;

    @Column(name = "retrieval_host")
    private String retrievalHost;

    @Column(name = "deposit_path")
    private String depositPath;

    @Column(name = "retrieval_path")
    private String retrievalPath;

    @Column(name = "deposit_share_name")
    private String depositShareName;

    @Column(name = "retrieval_share_name")
    private String retrievalShareName;

}
