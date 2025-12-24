package org.example.ariesbackendweb.MEC.DTOs;

import lombok.Data;
import org.example.ariesbackendweb.common.enums.TestStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class MecTestResponseDto {
    private UUID id;
    private UUID launchedBy;
    private String programName;
    private UUID programId;
    private int duration;
    private TestStatus status;
    private LocalDateTime launchedOn;
    private String retrievedFile;
    private String depositFile;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}