package org.example.civitaswebapp.service.communication;

import org.example.civitaswebapp.domain.MemberLanguage;
import org.example.civitaswebapp.dto.events.EventMessageDetails;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * {@code WhatsAppServiceImpl.sendEventPlanned} formats the message <em>before</em> resolving the
 * ContentSid, so null-guarding the template resolver alone would have left the event path throwing
 * a {@link NullPointerException} on the exhaustive switches here.
 */
class EventMessageFormatterTest {

    private final EventMessageFormatter formatter = new EventMessageFormatter(messageSource());

    private static ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("i18n/messages");
        source.setDefaultEncoding("UTF-8");
        return source;
    }

    private EventMessageDetails event(String location, String description) {
        return new EventMessageDetails(
                "Algemene Vergadering",
                "MEETING",
                LocalDateTime.of(2026, 3, 14, 19, 30),
                LocalDateTime.of(2026, 3, 14, 21, 0),
                location,
                description);
    }

    @Test
    void formatDateAndExtras_fallsBackToDutch_whenLanguageIsNull() {
        assertThatCode(() -> formatter.formatDateAndExtras(event(null, null), null))
                .doesNotThrowAnyException();

        assertThat(formatter.formatDateAndExtras(event(null, null), null))
                .isEqualTo(formatter.formatDateAndExtras(event(null, null), MemberLanguage.NL));
    }

    @Test
    void formatDateAndExtras_usesALanguageSpecificPattern() {
        String nl = formatter.formatDateAndExtras(event(null, null), MemberLanguage.NL);
        String tr = formatter.formatDateAndExtras(event(null, null), MemberLanguage.TR);
        String en = formatter.formatDateAndExtras(event(null, null), MemberLanguage.EN);

        assertThat(nl).contains("om 19:30");
        assertThat(tr).contains("saat 19:30");
        assertThat(en).contains("at 19:30");
        assertThat(nl).isNotEqualTo(tr).isNotEqualTo(en);
    }

    @Test
    void formatDateAndExtras_appendsLocationAndDescription_onlyWhenPresent() {
        assertThat(formatter.formatDateAndExtras(event(null, null), MemberLanguage.NL))
                .doesNotContain("\n");
        assertThat(formatter.formatDateAndExtras(event("   ", "  "), MemberLanguage.NL))
                .doesNotContain("\n");
        assertThat(formatter.formatDateAndExtras(event("Zaal De Kroon", "Jaarverslag"), MemberLanguage.NL))
                .contains("Zaal De Kroon")
                .contains("Jaarverslag");
    }
}
