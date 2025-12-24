package org.example.ariesbackendweb.MEC.DTOs;

import lombok.Data;

@Data
public class MecProgramResponseDto {
    private String id;
    private String code;
    private String depositHost;
    private String retrievalHost;
    private String depositPath;
    private String retrievalPath;
    private String depositShareName;
    private String retrievalShareName;
}
