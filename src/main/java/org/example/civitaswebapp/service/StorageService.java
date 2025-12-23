package org.example.civitaswebapp.service;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface StorageService {
    String store(MultipartFile file);

    Path getRootLocation();
}

