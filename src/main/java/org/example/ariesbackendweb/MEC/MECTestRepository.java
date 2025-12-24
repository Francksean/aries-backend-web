package org.example.ariesbackendweb.MEC;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MECTestRepository extends JpaRepository<MecTest, UUID> {
    List<MecTest> findByProgramId(UUID programId);
}
