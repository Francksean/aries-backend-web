package org.example.ariesbackendweb.MEC;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.example.ariesbackendweb.MEC.DTOs.LaunchAgentTestDto;
import org.example.ariesbackendweb.MEC.DTOs.MecTestResponseDto;
import org.example.ariesbackendweb.MEC.entities.MecProgram;
import org.example.ariesbackendweb.MEC.entities.MecTest;
import org.example.ariesbackendweb.MEC.mappers.MecTestMapper;
import org.example.ariesbackendweb.common.api.AgentService;
import org.example.ariesbackendweb.common.api.AgentWsService;
import org.example.ariesbackendweb.common.file.FileSystemStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;


@Service
@Data
@Slf4j
public class MecTestService {

    @Autowired
    MecTestRepository mecTestRepository;
    @Autowired
    MecProgramRepository mecProgramRepository;
    @Autowired
    FileSystemStorageService fileSystemStorageService;
    @Autowired
    AgentService agentService;
    @Autowired
    AgentWsService agentWsService;
    @Autowired
    MecTestMapper mecTestMapper;

    @Transactional
    public void launchTestCase(
            UUID userId,
            MecProgram program,
            MultipartFile depositFile

    ) {
        // on sauvegarde le fichier dans uploads
        String depositFileName = fileSystemStorageService.storeAndGetName(depositFile);

        // on crée d'abord l'enregistrement en BD
        MecTest newTest = registerMecTest(program, depositFileName, userId);

        executeTest(program, depositFile, newTest);
    }

    /**
     * rejouer un cas de test
     *
     * @param program
     * @param test
     */
    public void replayTest(MecProgram program, MecTest test) {
        // recupérer le fichier associé au cas de test
        String dFilePath = test.getDepositFile();
        Path path = Paths.get("uploads/" + dFilePath);
        byte[] content = null;
        try {
            content = Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        MultipartFile depositFile = new MockMultipartFile(dFilePath, content);
        executeTest(program, depositFile, test);
    }

    /**
     * pour exécuter le test : est utilisé à la fois pour un nouveau test et pour rejouer un test passé
     *
     * @param program
     * @param depositFile
     * @param test
     */
    private void executeTest(MecProgram program, MultipartFile depositFile, MecTest test) {
        // On crafte l'objet DTO à passer
        LaunchAgentTestDto launchAgentTestDto = getLaunchAgentTestDto(program, depositFile, test.getId());

        try {
            Path logDir = Paths.get("tests_logs");
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
            }
            // on crafte le nom du fichier de log
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

            String safeProgramCode = program.getCode()
                    .replaceAll("[^a-zA-Z0-9._-]", "_")
                    .replaceAll("_{2,}", "_");

            String logFileName = String.format("tests_logs/%s_%s.txt", timestamp, safeProgramCode);

            // on connecte sur les voies ws
            agentWsService.connect(test.getId().toString(), logFileName);
            // on lance le test
            agentService.launchMecTestAgent(launchAgentTestDto);

        } catch (IOException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    private static LaunchAgentTestDto getLaunchAgentTestDto(MecProgram program, MultipartFile depositFile, UUID testId) {
        LaunchAgentTestDto launchAgentTestDto = new LaunchAgentTestDto();
        launchAgentTestDto.setSessionId(testId);
        launchAgentTestDto.setFile(depositFile);
        launchAgentTestDto.setDPath(program.getDepositPath());
        launchAgentTestDto.setRPath(program.getRetrievalPath());
        launchAgentTestDto.setDHost(program.getDepositHost());
        launchAgentTestDto.setRHost(program.getRetrievalHost());
        launchAgentTestDto.setDShareName(program.getDepositShareName());
        launchAgentTestDto.setRShareName(program.getRetrievalShareName());
        return launchAgentTestDto;
    }

    @Transactional
    public MecTest registerMecTest(MecProgram program, String depositFileName, UUID userId) {
        MecTest newTest = new MecTest();
        newTest.setProgram(program);
        newTest.setDepositFile(depositFileName);
        newTest.setLaunchedBy(userId);
        newTest.setLaunchedOn(Timestamp.from(Instant.now()));
        mecTestRepository.save(newTest);
        return newTest;
    }

    @Transactional(readOnly = true)
    public List<MecTestResponseDto> getTestsByProgram(UUID programId) {
        log.info("Fetching MEC tests for program: {}", programId);

        return mecTestRepository.findByProgramId(programId).stream()
                .map(mecTestMapper::toResponseDto)
                .collect(Collectors.toList());
    }


    @Transactional
    public void storeTestResult(String filename, int duration, MecTest test) {
        test.setDuration(duration);
        test.setRetrievedFile(filename);
        mecTestRepository.save(test);
    }
}
