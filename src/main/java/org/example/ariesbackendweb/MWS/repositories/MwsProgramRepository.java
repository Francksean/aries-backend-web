package org.example.ariesbackendweb.MWS.repositories;

import org.example.ariesbackendweb.MEC.entities.MecProgram;
import org.example.ariesbackendweb.MWS.entities.MwsProgram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MwsProgramRepository extends JpaRepository<MwsProgram, UUID> {
    /**
     * Recherche un programme par son nom.
     * Utilisé pour vérifier l'unicité lors de la création.
     */
    Optional<MwsProgram> findByProgramName(String programName);

    /**
     * Liste tous les programmes actifs.
     */
    List<MwsProgram> findAllByActiveTrue();

    /**
     * Recherche des programmes par tag.
     */
    List<MwsProgram> findByTags_Tag(String tag);
}
