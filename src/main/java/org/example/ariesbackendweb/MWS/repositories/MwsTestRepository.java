package org.example.ariesbackendweb.MWS.repositories;

import org.example.ariesbackendweb.MWS.entities.MwsProgram;
import org.example.ariesbackendweb.MWS.entities.MwsTestRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MwsTestRepository extends JpaRepository<MwsTestRequest, UUID> {
}
