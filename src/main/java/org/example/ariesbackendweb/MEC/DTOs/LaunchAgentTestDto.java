package org.example.ariesbackendweb.MEC.DTOs;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Data
public class LaunchAgentTestDto {
    // Fichier à soumettre
    private MultipartFile file;

    private UUID sessionId;
    // Configuration du répertoire de dépôt (Deposit)
    private String dHost;
    private String dShareName;
    private String dPath;

    // Configuration du répertoire de récupération (Retrieve)
    private String rHost;
    private String rShareName;
    private String rPath;
}
