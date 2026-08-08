package org.example.civitaswebapp.service.communication;

import org.example.civitaswebapp.domain.MemberLanguage;
import org.example.civitaswebapp.dto.events.EventMessageDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Builds the {@code {{3}}} variable for the "civitas_event_planned" WhatsApp template: a single
 * string combining the event's start date/time - formatted per the member's language, since WhatsApp
 * template variables are inserted verbatim regardless of which language variant Twilio renders -
 * with the optional location/description appended, since WhatsApp templates don't support optional
 * fields.
 */
@Component
public class EventMessageFormatter {

    private static final Logger log = LoggerFactory.getLogger(EventMessageFormatter.class);

    private final MessageSource messageSource;

    public EventMessageFormatter(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String formatDateAndExtras(EventMessageDetails event, MemberLanguage language) {
        MemberLanguage effective = MemberLanguage.orDefault(language);
        if (language == null) {
            log.warn("Event attendee has no language set; formatting '{}' in {}",
                    event.title(), MemberLanguage.DEFAULT);
        }

        Locale locale = toLocale(effective);
        StringBuilder sb = new StringBuilder(formatDateTime(event.start(), effective, locale));

        if (event.location() != null && !event.location().isBlank()) {
            sb.append("\n").append(messageSource.getMessage(
                    "event.whatsapp.location", new Object[]{event.location()}, locale));
        }
        if (event.description() != null && !event.description().isBlank()) {
            sb.append("\n").append(messageSource.getMessage(
                    "event.whatsapp.description", new Object[]{event.description()}, locale));
        }
        return sb.toString();
    }

    private String formatDateTime(LocalDateTime start, MemberLanguage language, Locale locale) {
        String pattern = switch (language) {
            case NL -> "EEEE d MMMM 'om' HH:mm";
            case TR -> "d MMMM EEEE 'saat' HH:mm";
            case EN -> "EEEE d MMMM 'at' HH:mm";
        };
        return start.format(DateTimeFormatter.ofPattern(pattern, locale));
    }

    private Locale toLocale(MemberLanguage language) {
        return switch (language) {
            case NL -> Locale.forLanguageTag("nl-BE");
            case TR -> Locale.forLanguageTag("tr-TR");
            case EN -> Locale.forLanguageTag("en-GB");
        };
    }
}
