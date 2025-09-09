package org.example.civitaswebapp.dto;

import lombok.Data;
import org.example.civitaswebapp.domain.EventType;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class EventRequest {
    private String title;
    private String description;
    private LocalDateTime start;
    private LocalDateTime end;
    private String location;
    private EventType eventType;
    private List<Long> attendees; // member IDs
}
