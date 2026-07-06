package org.example.civitaswebapp.dto.events;

import org.example.civitaswebapp.domain.MemberLanguage;

/**
 * Immutable snapshot of the scalar Member fields the async WhatsApp listener needs (phone, name,
 * language), captured on the request thread. See {@link EventSavedEventDto} for why the managed
 * Member entity / attendees collection must never cross into the async listener.
 */
public record EventAttendeeContact(String phoneNumber, String name, MemberLanguage language) {
}
