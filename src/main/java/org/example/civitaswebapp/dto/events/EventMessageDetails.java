package org.example.civitaswebapp.dto.events;

import java.time.LocalDateTime;

/**
 * Immutable, entity-free snapshot of the event fields needed to compose a WhatsApp message.
 *
 * <p>Carrying scalars (never the managed {@code Event} entity) keeps the async notification path
 * from touching any Hibernate-managed collection — notably the bidirectional
 * {@code Event.attendees} / {@code Member.events} {@code PersistentSet} — which otherwise let the
 * request thread and the async listener race during collection loading.
 */
public record EventMessageDetails(String title,
                                  String eventType,
                                  LocalDateTime start,
                                  LocalDateTime end,
                                  String location,
                                  String description) {
}
