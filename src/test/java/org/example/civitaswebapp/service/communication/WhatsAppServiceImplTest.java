package org.example.civitaswebapp.service.communication;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Guards the phone-number validation added to the WhatsApp send path. Twilio rejects non-E.164
 * numbers at runtime (a local {@code 04...} instead of {@code +32...}), which previously surfaced
 * as a swallowed exception while the UI still claimed success. Validation now fails fast with a
 * deterministic {@link IllegalArgumentException} that the controller catches and reports.
 *
 * <p>Only the rejection paths are asserted here: a well-formed number would proceed to a real
 * Twilio API call, which is out of scope for a unit test.
 */
class WhatsAppServiceImplTest {

    private final WhatsAppServiceImpl service = new WhatsAppServiceImpl();

    @Test
    void sendPaymentLink_rejectsLocalNumberFormat() {
        assertThatThrownBy(() -> service.sendPaymentLink("0470123456", "https://pay.example/abc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("international");
    }

    @Test
    void sendPaymentLink_rejectsNullNumber() {
        assertThatThrownBy(() -> service.sendPaymentLink(null, "https://pay.example/abc"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sendPaymentLink_rejectsBlankNumber() {
        assertThatThrownBy(() -> service.sendPaymentLink("   ", "https://pay.example/abc"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sendPaymentLink_doesNotRejectWellFormedInternationalNumberDuringValidation() {
        // The validation guard itself must accept a +E.164 number. We cannot let the call reach
        // Twilio, so we only assert the guard does not throw the *validation* error; any later
        // Twilio/network failure is a different exception type and acceptable here.
        assertThatCode(() -> {
            try {
                service.sendPaymentLink("+32470123456", "https://pay.example/abc");
            } catch (IllegalArgumentException validationError) {
                throw validationError; // re-throw only validation failures
            } catch (RuntimeException twilioOrNetwork) {
                // expected: validation passed, the call attempted to reach Twilio
            }
        }).doesNotThrowAnyException();
    }
}
