package org.example.civitaswebapp.dto.events;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Published on event save and consumed by the async notification listener.
 *
 * <p>Carries everything the listener needs as scalars/immutable data — including the attendees'
 * phone numbers, captured on the request thread while the members are already loaded. The listener
 * must therefore never reload the {@code Event} or touch its {@code attendees} collection, so the
 * request thread and the async thread never race on the same Hibernate {@code PersistentSet}.
 */
public record EventSavedEventDto(Long eventId,
                                 String title,
                                 String description,
                                 LocalDateTime start,
                                 LocalDateTime end,
                                 String location,
                                 String eventType,
                                 Long createdByUserId,
                                 boolean isNew,
                                 List<String> attendeePhoneNumbers)
{
}
