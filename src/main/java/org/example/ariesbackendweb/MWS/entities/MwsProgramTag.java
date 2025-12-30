package org.example.ariesbackendweb.MWS.entities;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "mws_program_tags")
@IdClass(MwsServiceTagId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MwsProgramTag {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private MwsProgram program;

    @Id
    @Column(nullable = false, length = 50)
    private String tag;
}

// Classe d'ID composite
class MwsServiceTagId implements java.io.Serializable {
    private UUID program;
    private String tag;

    public MwsServiceTagId() {}

    public MwsServiceTagId(UUID program, String tag) {
        this.program = program;
        this.tag = tag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MwsServiceTagId that = (MwsServiceTagId) o;

        if (!Objects.equals(program, that.program)) return false;
        return Objects.equals(tag, that.tag);
    }

    @Override
    public int hashCode() {
        int result = program != null ? program.hashCode() : 0;
        result = 31 * result + (tag != null ? tag.hashCode() : 0);
        return result;
    }
}
