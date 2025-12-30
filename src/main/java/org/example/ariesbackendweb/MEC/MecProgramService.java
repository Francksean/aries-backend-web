package org.example.ariesbackendweb.MEC;

import lombok.extern.slf4j.Slf4j;
import org.example.ariesbackendweb.MEC.DTOs.MecProgramRequestDto;
import org.example.ariesbackendweb.MEC.DTOs.MecProgramResponseDto;
import org.example.ariesbackendweb.MEC.DTOs.MecProgramUpdateDto;
import org.example.ariesbackendweb.MEC.entities.MecProgram;
import org.example.ariesbackendweb.MEC.mappers.MecProgramMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MecProgramService {

    @Autowired
    private MecProgramRepository mecProgramRepository;
    @Autowired
    private MecProgramMapper mecProgramMapper;

    @Transactional
    public MecProgramResponseDto createProgram(MecProgramRequestDto requestDto) {
        log.info("Creating MEC program with code: {}", requestDto.getCode());

        // Check if program with same code already exists
        if (mecProgramRepository.existsByCode(requestDto.getCode())) {
            throw new IllegalArgumentException("Program with code " + requestDto.getCode() + " already exists");
        }

        MecProgram program = mecProgramMapper.toEntity(requestDto);
        program = mecProgramRepository.save(program);

        log.info("MEC program created successfully with id: {}", program.getId());
        return mecProgramMapper.toResponseDto(program);
    }

    @Transactional(readOnly = true)
    public MecProgramResponseDto getProgramById(UUID id) {
        log.info("Fetching MEC program with id: {}", id);

        MecProgram program = mecProgramRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("MEC program not found with id: " + id));

        return mecProgramMapper.toResponseDto(program);
    }

    @Transactional(readOnly = true)
    public MecProgramResponseDto getProgramByCode(String code) {
        log.info("Fetching MEC program with code: {}", code);

        MecProgram program = mecProgramRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("MEC program not found with code: " + code));

        return mecProgramMapper.toResponseDto(program);
    }

    @Transactional(readOnly = true)
    public List<MecProgramResponseDto> getAllPrograms() {
        log.info("Fetching all MEC programs");

        return mecProgramRepository.findAll().stream()
                .map(mecProgramMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MecProgramResponseDto> searchProgramsByCode(String code) {
        log.info("Searching MEC programs with code containing: {}", code);

        return mecProgramRepository.findAllByCodeContainingIgnoreCase(code).stream()
                .map(mecProgramMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public MecProgramResponseDto updateProgram(UUID id, MecProgramUpdateDto updateDto) {
        log.info("Updating MEC program with id: {}", id);

        MecProgram program = mecProgramRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("MEC program not found with id: " + id));

        // Check if new code already exists (if code is being updated)
        if (updateDto.getCode() != null &&
                !updateDto.getCode().equals(program.getCode()) &&
                mecProgramRepository.existsByCode(updateDto.getCode())) {
            throw new IllegalArgumentException("Program with code " + updateDto.getCode() + " already exists");
        }

        mecProgramMapper.updateEntityFromDto(updateDto, program);
        program = mecProgramRepository.save(program);

        log.info("MEC program updated successfully with id: {}", id);
        return mecProgramMapper.toResponseDto(program);
    }

    @Transactional
    public void deleteProgram(UUID id) {
        log.info("Deleting MEC program with id: {}", id);

        if (!mecProgramRepository.existsById(id)) {
            throw new IllegalArgumentException("MEC program not found with id: " + id);
        }

        // Check if there are any tests associated with this program
        // You might want to add this check if you have a relationship

        try {
            mecProgramRepository.deleteById(id);
            log.info("MEC program deleted successfully with id: {}", id);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Cannot delete program. There might be associated tests.", e);
        }
    }

    @Transactional(readOnly = true)
    public boolean programExists(UUID id) {
        return mecProgramRepository.existsById(id);
    }
}