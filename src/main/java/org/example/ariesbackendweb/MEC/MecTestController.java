package org.example.ariesbackendweb.MEC;

import org.example.ariesbackendweb.MEC.DTOs.LaunchTestDto;
import org.example.ariesbackendweb.MEC.DTOs.MecTestResponseDto;
import org.example.ariesbackendweb.MEC.entities.MecProgram;
import org.example.ariesbackendweb.MEC.entities.MecTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/mec/tests")
public class MecTestController {

    @Autowired
    private MecTestService mecService;

    @PostMapping("/launch/{programId}")
    public ResponseEntity<?> launch(
            @PathVariable("programId") MecProgram program,
            @ModelAttribute LaunchTestDto dto
    ) {
        // Lancer le test via le service
        mecService.launchTestCase(dto.getUserId(), program, dto.getDepositFile());

        return ResponseEntity.ok().build();
    }

    @GetMapping("/replay/{programId}/{testId}")
    public ResponseEntity<?> replay(@PathVariable("programId") MecProgram program, @PathVariable("testId") MecTest test){
        mecService.replayTest(program, test);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{programId}")
    public ResponseEntity<List<MecTestResponseDto>> getTestsByProgram(@PathVariable UUID programId) {
        List<MecTestResponseDto> responses = mecService.getTestsByProgram(programId);
        return ResponseEntity.ok(responses);
    }

    /**
     * C'est l'endponint qui permet à l'orchestrateur de récupérer les données du test.
     */
    @PostMapping("/result/{testId}")
    public ResponseEntity<?> result(@PathVariable("testId") MecTest test, @RequestBody String filename, @RequestBody int duration) {
        mecService.storeTestResult(filename, duration, test);
        return ResponseEntity.ok().build();
    }

}
