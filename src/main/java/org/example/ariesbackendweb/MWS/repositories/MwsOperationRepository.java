package org.example.ariesbackendweb.MWS.repositories;

import org.example.ariesbackendweb.MWS.entities.MwsOperation;
import org.example.ariesbackendweb.MWS.entities.MwsProgram;
import org.example.ariesbackendweb.MWS.entities.MwsProgramEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MwsOperationRepository extends JpaRepository<MwsOperation, UUID> {
    /**
     * Supprime toutes les opérations d'un programme.
     * Utilisé lors de la resynchronisation.
     */
    int deleteByProgram(MwsProgram service);

    /**
     * Recherche une opération par son nom dans un programme donné.
     */
    Optional<MwsOperation> findByProgramAndOperationName(
            MwsProgram service,
            String operationName
    );
}
