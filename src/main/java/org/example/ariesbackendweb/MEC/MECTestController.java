package org.example.ariesbackendweb.MEC;

import org.example.ariesbackendweb.MEC.DTOs.LaunchTestDto;
import org.example.ariesbackendweb.MEC.DTOs.MecTestResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/mec/tests")
public class MECTestController {

    @Autowired
    private MECTestService mecService;

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

}
