package org.example.ariesbackendweb.MEC.DTOs;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class MecProgramRequestDto {
    @NotBlank(message = "Code is required")
    private String code;

    @NotBlank(message = "Deposit host is required")
    private String depositHost;

    @NotBlank(message = "Retrieval host is required")
    private String retrievalHost;

    @NotBlank(message = "Deposit path is required")
    private String depositPath;

    @NotBlank(message = "Retrieval path is required")
    private String retrievalPath;

    @NotBlank(message = "Deposit share name is required")
    private String depositShareName;

    @NotBlank(message = "Retrieval share name is required")
    private String retrievalShareName;
}
