package org.example.civitaswebapp.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the null fallback that keeps the WhatsApp send path alive for members whose row predates
 * the {@code language} column and therefore still holds NULL.
 */
class MemberLanguageTest {

    @Test
    void defaultIsDutch() {
        // Civitas targets Flanders — changing this silently changes which template members receive.
        assertThat(MemberLanguage.DEFAULT).isEqualTo(MemberLanguage.NL);
    }

    @Test
    void orDefault_fallsBackWhenNull() {
        assertThat(MemberLanguage.orDefault(null)).isEqualTo(MemberLanguage.NL);
    }

    @Test
    void orDefault_passesThroughEveryConstant() {
        for (MemberLanguage language : MemberLanguage.values()) {
            assertThat(MemberLanguage.orDefault(language)).isEqualTo(language);
        }
    }
}
