package org.example.ariesbackendweb.MEC;

import org.example.ariesbackendweb.MEC.entities.MecProgram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MecProgramRepository extends JpaRepository<MecProgram, UUID> {
    Optional<MecProgram> findByCode(String code);

    boolean existsByCode(String code);

    List<MecProgram> findAllByCodeContainingIgnoreCase(String code);
}
