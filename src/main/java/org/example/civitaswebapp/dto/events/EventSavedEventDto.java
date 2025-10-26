package org.example.civitaswebapp.dto.events;
import java.time.LocalDateTime;

public record EventSavedEventDto(Long eventId,
                                 String title,
                                 String description,
                                 LocalDateTime start,
                                 LocalDateTime end,
                                 Long createdByUserId,
                                 boolean isNew
                                 )
{
}
