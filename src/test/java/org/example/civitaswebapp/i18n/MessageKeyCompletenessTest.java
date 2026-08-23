package org.example.civitaswebapp.i18n;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The four bundles have drifted apart before (they differ in length), and a key missing from one of
 * them surfaces as a raw {@code ??key??} placeholder in the UI or a
 * {@code NoSuchMessageException} from {@code MessageSource.getMessage} at the exact moment a
 * WhatsApp send has already failed. Cheap to guard, expensive to discover in production.
 */
class MessageKeyCompletenessTest {

    private static final List<String> REQUIRED_KEYS = List.of(
            // Member language selection (member form + bean validation)
            "member.form.language",
            "member.language.NL",
            "member.language.EN",
            "member.language.TR",
            "member.language.required",
            // WhatsApp delivery failure classification
            "payment.whatsapp.error.phoneMissing",
            "payment.whatsapp.error.phoneInvalid",
            "payment.whatsapp.error.provider",
            "payment.whatsapp.error.system",
            // Still the client-side fallback in memberDetails.js
            "payment.whatsapp.failed"
    );

    @ParameterizedTest
    @ValueSource(strings = {
            "i18n/messages.properties",
            "i18n/messages_en.properties",
            "i18n/messages_nl.properties",
            "i18n/messages_tr.properties"
    })
    void everyBundleDefinesTheRequiredKeys(String bundle) throws Exception {
        Properties properties = load(bundle);

        assertThat(properties.stringPropertyNames())
                .as("keys missing from %s", bundle)
                .containsAll(REQUIRED_KEYS);

        for (String key : REQUIRED_KEYS) {
            assertThat(properties.getProperty(key))
                    .as("%s in %s must not be blank", key, bundle)
                    .isNotBlank();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "i18n/messages.properties",
            "i18n/messages_en.properties",
            "i18n/messages_nl.properties",
            "i18n/messages_tr.properties"
    })
    void providerErrorKeepsItsMessageFormatPlaceholder(String bundle) throws Exception {
        // This one is resolved with an argument, so it runs through MessageFormat: it must keep {0}
        // and stay free of single quotes, which MessageFormat would otherwise swallow.
        String value = load(bundle).getProperty("payment.whatsapp.error.provider");

        assertThat(value).as("%s must interpolate the Twilio error code", bundle).contains("{0}");
        assertThat(value).as("%s: apostrophes must be doubled for MessageFormat", bundle)
                .doesNotContain("'");
    }

    private Properties load(String bundle) throws Exception {
        Properties properties = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(bundle)) {
            assertThat(in).as("bundle %s must exist on the classpath", bundle).isNotNull();
            properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        return properties;
    }
}
