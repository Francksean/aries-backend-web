package org.example.ariesbackendweb.MEC;

import jakarta.persistence.*;
import lombok.*;
import org.example.ariesbackendweb.common.entities.Test;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "MEC_Tests")
public class MecTest extends Test {

    @Column(name = "retrieved_file_path")
    public String retrievedFile;

    @Column(name = "deposit_file_path", nullable = false)
    public String depositFile;
}
