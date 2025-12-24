package org.example.ariesbackendweb.MEC;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@Slf4j
@Transactional
public class FileAvailableEventListener {

    @Autowired
    MECTestRepository mecTestRepository;

    @EventListener
    public void onFileAvailable(FileAvailableEvent fileAvailableEvent) {

        UUID testId = UUID.fromString(fileAvailableEvent.getTestId());
        String fileName = fileAvailableEvent.getAvailableFileName();
        int duration = (int) fileAvailableEvent.getDuration();

        log.info("*".repeat(30));
        log.info("INFORMATION RECUES NOM DU FICHIER {} et DURATION {} ", fileName, duration);
        log.info("*".repeat(30));

        MecTest test = mecTestRepository.findById(testId).orElse(null);

        assert test != null;
        test.setRetrievedFile(fileName);
        test.setDuration(duration);
        mecTestRepository.save(test);

    }
}
