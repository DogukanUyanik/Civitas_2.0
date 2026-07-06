package org.example.civitaswebapp.service.communication;

import org.example.civitaswebapp.domain.MemberLanguage;
import org.example.civitaswebapp.dto.events.EventMessageDetails;
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

    private final MessageSource messageSource;

    public EventMessageFormatter(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String formatDateAndExtras(EventMessageDetails event, MemberLanguage language) {
        Locale locale = toLocale(language);
        StringBuilder sb = new StringBuilder(formatDateTime(event.start(), language, locale));

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
