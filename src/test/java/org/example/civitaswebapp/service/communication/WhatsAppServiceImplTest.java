package org.example.civitaswebapp.service.communication;

import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.domain.MemberLanguage;
import org.example.civitaswebapp.dto.events.EventMessageDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Guards the phone-number validation added to the WhatsApp send path. Twilio rejects non-E.164
 * numbers at runtime (a local {@code 04...} instead of {@code +32...}), which previously surfaced
 * as a swallowed exception while the UI still claimed success. Validation now fails fast with a
 * deterministic {@link IllegalArgumentException} that the controller catches and reports.
 *
 * <p>Only the rejection paths are asserted here: a well-formed number would proceed to a real
 * Twilio API call, which is out of scope for a unit test.
 */
@ExtendWith(MockitoExtension.class)
class WhatsAppServiceImplTest {

    @Mock
    private WhatsAppTemplateResolver templateResolver;
    @Mock
    private EventMessageFormatter eventMessageFormatter;

    @InjectMocks
    private WhatsAppServiceImpl service;

    private Member memberWithPhone(String phone) {
        return Member.builder().firstName("Test").lastName("Member").phoneNumber(phone).build();
    }

    @Test
    void sendPaymentLink_rejectsLocalNumberFormat() {
        assertThatThrownBy(() -> service.sendPaymentLink(memberWithPhone("0470123456"), "https://pay.example/abc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("international");
    }

    @Test
    void sendPaymentLink_rejectsNullNumber() {
        assertThatThrownBy(() -> service.sendPaymentLink(memberWithPhone(null), "https://pay.example/abc"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sendPaymentLink_rejectsBlankNumber() {
        assertThatThrownBy(() -> service.sendPaymentLink(memberWithPhone("   "), "https://pay.example/abc"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sendPaymentLink_doesNotRejectWellFormedInternationalNumberDuringValidation() {
        // The validation guard itself must accept a +E.164 number. We cannot let the call reach
        // Twilio, so we only assert the guard does not throw the *validation* error; any later
        // Twilio/network failure is a different exception type and acceptable here.
        assertThatCode(() -> {
            try {
                service.sendPaymentLink(memberWithPhone("+32470123456"), "https://pay.example/abc");
            } catch (IllegalArgumentException validationError) {
                throw validationError; // re-throw only validation failures
            } catch (RuntimeException twilioOrNetwork) {
                // expected: validation passed, the call attempted to reach Twilio
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void sendPaymentLink_delegatesNullLanguageToTheResolver() {
        // The null fallback lives in exactly one place (the resolver), so the service must pass the
        // member's language through untouched rather than normalizing it at each of its call sites.
        Member member = memberWithPhone("+32470123456");
        member.setLanguage(null);
        when(templateResolver.resolve("payment-link", null)).thenReturn("HX_nl");

        try {
            service.sendPaymentLink(member, "https://pay.example/abc");
        } catch (RuntimeException twilioOrNetwork) {
            // expected: the send itself cannot reach Twilio in a unit test
        }

        verify(templateResolver).resolve("payment-link", null);
    }

    @Test
    void sendEventPlanned_formatsBeforeResolvingTheTemplate() {
        // Ordering matters: the formatter switches exhaustively over MemberLanguage, so it needs its
        // own null guard — the resolver's would never be reached for a null-language attendee.
        EventMessageDetails event = new EventMessageDetails(
                "Algemene Vergadering", "MEETING",
                LocalDateTime.of(2026, 3, 14, 19, 30), LocalDateTime.of(2026, 3, 14, 21, 0),
                null, null);
        when(eventMessageFormatter.formatDateAndExtras(any(), any())).thenReturn("14 maart om 19:30");
        when(templateResolver.resolve(eq("event-planned"), any())).thenReturn("HX_event_nl");

        try {
            service.sendEventPlanned("+32470123456", "Test Member", MemberLanguage.NL, event);
        } catch (RuntimeException twilioOrNetwork) {
            // expected: the send itself cannot reach Twilio in a unit test
        }

        InOrder order = inOrder(eventMessageFormatter, templateResolver);
        order.verify(eventMessageFormatter).formatDateAndExtras(event, MemberLanguage.NL);
        order.verify(templateResolver).resolve("event-planned", MemberLanguage.NL);
    }
}
