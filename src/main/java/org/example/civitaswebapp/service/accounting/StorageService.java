package org.example.civitaswebapp.service.accounting;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface StorageService {
    String store(MultipartFile file);

    Path getRootLocation();
}

