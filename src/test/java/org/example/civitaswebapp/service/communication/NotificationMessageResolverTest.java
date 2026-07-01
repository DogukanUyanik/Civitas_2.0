package org.example.civitaswebapp.service.communication;

import org.example.civitaswebapp.domain.Notification;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that notification content is resolved from the real {@code i18n/messages*} bundles in
 * the viewer's locale, with runtime arguments substituted. This locks in that the translation keys
 * the listeners emit actually exist in Dutch, English, and Turkish (a missing key would fall back
 * to the raw key, which these assertions would catch).
 */
class NotificationMessageResolverTest {

    private final NotificationMessageResolver resolver = new NotificationMessageResolver(messageSource());

    private static ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
        ms.setBasename("i18n/messages");
        ms.setDefaultEncoding("UTF-8");
        return ms;
    }

    private Notification memberCreated() {
        return Notification.builder()
                .titleKey("notification.member.created.title")
                .messageKey("notification.member.created.message")
                .messageArgs(List.of("Ali Khan"))
                .build();
    }

    @Test
    void resolvesDutchTitleAndMessageWithArgs() {
        Notification n = memberCreated();
        Locale nl = Locale.forLanguageTag("nl");

        assertThat(resolver.resolveTitle(n, nl)).isEqualTo("Nieuw lid toegevoegd");
        assertThat(resolver.resolveMessage(n, nl)).isEqualTo("Lid Ali Khan is toegevoegd.");
    }

    @Test
    void resolvesEnglishTitleAndMessageWithArgs() {
        Notification n = memberCreated();

        assertThat(resolver.resolveTitle(n, Locale.ENGLISH)).isEqualTo("New member added");
        assertThat(resolver.resolveMessage(n, Locale.ENGLISH)).isEqualTo("Member Ali Khan has been added.");
    }

    @Test
    void resolvesTurkishTitleAndMessageWithArgs() {
        Notification n = memberCreated();
        Locale tr = Locale.forLanguageTag("tr");

        assertThat(resolver.resolveTitle(n, tr)).isEqualTo("Yeni üye eklendi");
        assertThat(resolver.resolveMessage(n, tr)).isEqualTo("Ali Khan adlı üye eklendi.");
    }

    @Test
    void resolvesTwoArgumentStatusMessage() {
        Notification n = Notification.builder()
                .titleKey("notification.transaction.statusChanged.title")
                .messageKey("notification.transaction.statusChanged.message")
                .messageArgs(List.of("42", "SUCCEEDED"))
                .build();

        assertThat(resolver.resolveMessage(n, Locale.ENGLISH)).isEqualTo("Transaction 42 is now SUCCEEDED.");
    }

    @Test
    void missingKeyDegradesToTheKeyItself() {
        Notification n = Notification.builder()
                .titleKey("notification.does.not.exist")
                .messageKey("notification.also.missing")
                .messageArgs(List.of())
                .build();

        assertThat(resolver.resolveTitle(n, Locale.ENGLISH)).isEqualTo("notification.does.not.exist");
        assertThat(resolver.resolveMessage(n, Locale.ENGLISH)).isEqualTo("notification.also.missing");
    }
}
