package org.example.ariesbackendweb.MEC;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.ariesbackendweb.MEC.DTOs.MecProgramRequestDto;
import org.example.ariesbackendweb.MEC.DTOs.MecProgramResponseDto;
import org.example.ariesbackendweb.MEC.DTOs.MecProgramUpdateDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/mec/programs")
public class MecProgramController {

    @Autowired
    private MecProgramService mecProgramService;

    @PostMapping
    public ResponseEntity<MecProgramResponseDto> createProgram(@Valid @RequestBody MecProgramRequestDto requestDto) {
        MecProgramResponseDto response = mecProgramService.createProgram(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MecProgramResponseDto> getProgramById(@PathVariable UUID id) {
        MecProgramResponseDto response = mecProgramService.getProgramById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<MecProgramResponseDto> getProgramByCode(@PathVariable String code) {
        MecProgramResponseDto response = mecProgramService.getProgramByCode(code);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<MecProgramResponseDto>> getAllPrograms() {
        List<MecProgramResponseDto> responses = mecProgramService.getAllPrograms();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/search")
    public ResponseEntity<List<MecProgramResponseDto>> searchPrograms(@RequestParam(required = false) String code) {
        List<MecProgramResponseDto> responses;
        if (code != null && !code.trim().isEmpty()) {
            responses = mecProgramService.searchProgramsByCode(code);
        } else {
            responses = mecProgramService.getAllPrograms();
        }
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MecProgramResponseDto> updateProgram(
            @PathVariable UUID id,
            @Valid @RequestBody MecProgramUpdateDto updateDto) {
        MecProgramResponseDto response = mecProgramService.updateProgram(id, updateDto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProgram(@PathVariable UUID id) {
        mecProgramService.deleteProgram(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/exists")
    public ResponseEntity<Boolean> programExists(@PathVariable UUID id) {
        boolean exists = mecProgramService.programExists(id);
        return ResponseEntity.ok(exists);
    }
}