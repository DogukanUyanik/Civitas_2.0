package org.example.civitaswebapp.dto.events;

import java.time.LocalDateTime;

public record EventResponseDto(
        Long id,
        String title,
        String description,
        LocalDateTime start,
        LocalDateTime end,
        String location,
        String eventType
) {}
