package org.example.ariesbackendweb.MEC.DTOs;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Data
public class LaunchTestDto {
    private MultipartFile depositFile;
    private UUID userId;
}
