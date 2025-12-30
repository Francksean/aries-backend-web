package org.example.ariesbackendweb.MWS.repositories;

import org.example.ariesbackendweb.MWS.entities.MwsProgram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MwsProgramEndpointRepository extends JpaRepository<MwsProgram, UUID> {

}
