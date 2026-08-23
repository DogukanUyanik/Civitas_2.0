package org.example.civitaswebapp.service.communication;

import org.example.civitaswebapp.domain.MemberLanguage;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A member whose {@code language} is NULL must still receive a message. Previously
 * {@code resolve} dereferenced the enum directly and threw a {@link NullPointerException} deep in
 * the send path, which the controller then reported to the admin as a phone-number problem.
 *
 * <p>The fallback must not swallow genuine configuration errors: a missing ContentSid is a
 * deployment problem and still has to fail loudly.
 */
class WhatsAppTemplateResolverTest {

    private final WhatsAppTemplateResolver resolver = new WhatsAppTemplateResolver(Map.of(
            "payment-link", Map.of("nl", "HX_nl", "en", "HX_en", "tr", "HX_tr"),
            "payment-failed", Map.of("nl", "HX_failed_nl", "en", "   ")
    ));

    @Test
    void resolve_fallsBackToDutch_whenLanguageIsNull() {
        assertThat(resolver.resolve("payment-link", null)).isEqualTo("HX_nl");
    }

    @Test
    void resolve_returnsTheLanguageSpecificSid() {
        assertThat(resolver.resolve("payment-link", MemberLanguage.NL)).isEqualTo("HX_nl");
        assertThat(resolver.resolve("payment-link", MemberLanguage.EN)).isEqualTo("HX_en");
        assertThat(resolver.resolve("payment-link", MemberLanguage.TR)).isEqualTo("HX_tr");
    }

    @Test
    void resolve_throws_whenTemplateKeyIsUnknown() {
        assertThatThrownBy(() -> resolver.resolve("no-such-template", MemberLanguage.NL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no-such-template");
    }

    @Test
    void resolve_throws_whenSidIsBlank() {
        assertThatThrownBy(() -> resolver.resolve("payment-failed", MemberLanguage.EN))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EN");
    }

    @Test
    void resolve_throws_whenLanguageVariantIsNotConfigured() {
        assertThatThrownBy(() -> resolver.resolve("payment-failed", MemberLanguage.TR))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TR");
    }

    @Test
    void resolve_stillThrows_whenLanguageIsNullAndTemplateIsUnknown() {
        // The null fallback must not mask a configuration error as a successful resolution.
        assertThatThrownBy(() -> resolver.resolve("no-such-template", null))
                .isInstanceOf(IllegalStateException.class);
    }
}
