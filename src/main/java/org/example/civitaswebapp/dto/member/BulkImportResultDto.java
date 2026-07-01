package org.example.civitaswebapp.dto.member;

import java.util.List;

public record BulkImportResultDto(
        int successCount,
        int skippedCount,
        List<String> errors
) {}
