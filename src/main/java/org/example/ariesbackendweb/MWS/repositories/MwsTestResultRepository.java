package org.example.ariesbackendweb.MWS.repositories;

import org.example.ariesbackendweb.MWS.entities.MwsTestResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MwsTestResultRepository  extends JpaRepository<MwsTestResult, UUID> {
}
