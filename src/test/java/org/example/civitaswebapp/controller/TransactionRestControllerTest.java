package org.example.civitaswebapp.controller;

import com.twilio.exception.ApiConnectionException;
import com.twilio.exception.ApiException;
import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.domain.Transaction;
import org.example.civitaswebapp.domain.TransactionType;
import org.example.civitaswebapp.service.MemberService;
import org.example.civitaswebapp.service.MyUserService;
import org.example.civitaswebapp.service.TransactionService;
import org.example.civitaswebapp.service.communication.WhatsAppService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Guards the Twilio failure-handling contract of {@code /transactions/send-payment}: the
 * transaction is created regardless of delivery, but the response must expose a truthful
 * {@code whatsappSuccess} flag so the UI never shows a false "sent via WhatsApp" toast when Twilio
 * actually rejected the number (e.g. a local {@code 04...} instead of {@code +32...}).
 *
 * <p>It also guards the <em>classification</em> of that failure. The admin used to be told to check
 * the phone number no matter what actually went wrong, so each cause must map to its own
 * {@code whatsapp_error_code}. Assertions target the code and the message key, never the translated
 * prose — {@link MessageSource} is stubbed to echo the key back so rewording a translation cannot
 * break these tests.
 */
@ExtendWith(MockitoExtension.class)
class TransactionRestControllerTest {

    @Mock
    private TransactionService transactionService;
    @Mock
    private MemberService memberService;
    @Mock
    private WhatsAppService whatsAppService;
    @Mock
    private MyUserService myUserService;
    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private TransactionRestController controller;

    /** Makes {@code whatsapp_error} carry the message key, so assertions stay translation-agnostic. */
    private void stubMessageSourceEchoingKeys() {
        when(messageSource.getMessage(anyString(), any(), any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void assertWhatsappFailure(Map<String, Object> body, String expectedCode, String expectedKey) {
        // The transaction was still created, but delivery must NOT be reported as successful.
        assertThat(body.get("success")).isEqualTo(true);
        assertThat(body.get("whatsappSuccess")).isEqualTo(false);
        assertThat(body.get("whatsapp_error_code")).isEqualTo(expectedCode);
        assertThat(body.get("whatsapp_error")).isEqualTo(expectedKey);
    }

    private Member stubHappyTransactionPath(String phoneNumber) {
        Member member = new Member();
        member.setId(1L);
        member.setPhoneNumber(phoneNumber);

        Transaction transaction = mock(Transaction.class);
        when(transaction.getId()).thenReturn(99L);

        when(memberService.findById(1L)).thenReturn(Optional.of(member));
        when(myUserService.getLoggedInUser()).thenReturn(new MyUser());
        when(transactionService.createTransaction(any(), any(Double.class), any(TransactionType.class), any()))
                .thenReturn(transaction);
        when(transactionService.generateStripePaymentLink(transaction)).thenReturn("https://pay.example/abc");
        return member;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> bodyOf(ResponseEntity<Map<String, Object>> response) {
        return response.getBody();
    }

    @Test
    void sendPayment_reportsWhatsappSuccess_whenDeliverySucceeds() {
        Member member = stubHappyTransactionPath("+32470123456");

        ResponseEntity<Map<String, Object>> response =
                controller.sendPayment(1L, 50.0, "EUR", "MEMBERSHIP_FEE", null);

        Map<String, Object> body = bodyOf(response);
        assertThat(body.get("success")).isEqualTo(true);
        assertThat(body.get("whatsappSuccess")).isEqualTo(true);
        verify(whatsAppService).sendPaymentLink(member, "https://pay.example/abc");
    }

    @Test
    void sendPayment_reportsPhoneInvalid_whenNumberIsNotInternational() {
        stubHappyTransactionPath("0470123456");
        stubMessageSourceEchoingKeys();
        doThrow(new IllegalArgumentException("Invalid phone number format"))
                .when(whatsAppService).sendPaymentLink(any(Member.class), anyString());

        ResponseEntity<Map<String, Object>> response =
                controller.sendPayment(1L, 50.0, "EUR", "MEMBERSHIP_FEE", null);

        assertWhatsappFailure(bodyOf(response), "PHONE_INVALID", "payment.whatsapp.error.phoneInvalid");
    }

    @Test
    void sendPayment_reportsPhoneMissing_whenMemberHasNoPhone() {
        stubHappyTransactionPath(null);
        stubMessageSourceEchoingKeys();

        ResponseEntity<Map<String, Object>> response =
                controller.sendPayment(1L, 50.0, "EUR", "MEMBERSHIP_FEE", null);

        assertWhatsappFailure(bodyOf(response), "PHONE_MISSING", "payment.whatsapp.error.phoneMissing");
        verify(whatsAppService, never()).sendPaymentLink(any(), any());
    }

    @Test
    void sendPayment_reportsTwilioRejected_whenProviderRefusesTheMessage() {
        stubHappyTransactionPath("+32470123456");
        stubMessageSourceEchoingKeys();
        doThrow(new ApiException("Invalid To number", 21211, null, 400, null))
                .when(whatsAppService).sendPaymentLink(any(Member.class), anyString());

        ResponseEntity<Map<String, Object>> response =
                controller.sendPayment(1L, 50.0, "EUR", "MEMBERSHIP_FEE", null);

        assertWhatsappFailure(bodyOf(response), "TWILIO_REJECTED", "payment.whatsapp.error.provider");
    }

    @Test
    void sendPayment_reportsTwilioRejected_whenProviderIsUnreachable() {
        // A network failure is still an external problem, not a misconfiguration — it must not be
        // reported to the admin as one.
        stubHappyTransactionPath("+32470123456");
        stubMessageSourceEchoingKeys();
        doThrow(new ApiConnectionException("Connection refused"))
                .when(whatsAppService).sendPaymentLink(any(Member.class), anyString());

        ResponseEntity<Map<String, Object>> response =
                controller.sendPayment(1L, 50.0, "EUR", "MEMBERSHIP_FEE", null);

        assertWhatsappFailure(bodyOf(response), "TWILIO_REJECTED", "payment.whatsapp.error.provider");
    }

    @Test
    void sendPayment_reportsSystemError_whenTemplateIsNotConfigured() {
        stubHappyTransactionPath("+32470123456");
        stubMessageSourceEchoingKeys();
        doThrow(new IllegalStateException("No Twilio content SID configured for template 'payment-link'"))
                .when(whatsAppService).sendPaymentLink(any(Member.class), anyString());

        ResponseEntity<Map<String, Object>> response =
                controller.sendPayment(1L, 50.0, "EUR", "MEMBERSHIP_FEE", null);

        assertWhatsappFailure(bodyOf(response), "SYSTEM_ERROR", "payment.whatsapp.error.system");
    }

    @Test
    void sendPayment_reportsSystemError_whenFailureIsUnexpected() {
        // The catch-all backstop must still keep whatsappSuccess false rather than 500-ing the
        // request: the transaction itself was created successfully.
        stubHappyTransactionPath("+32470123456");
        stubMessageSourceEchoingKeys();
        doThrow(new RuntimeException("boom"))
                .when(whatsAppService).sendPaymentLink(any(Member.class), anyString());

        ResponseEntity<Map<String, Object>> response =
                controller.sendPayment(1L, 50.0, "EUR", "MEMBERSHIP_FEE", null);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertWhatsappFailure(bodyOf(response), "SYSTEM_ERROR", "payment.whatsapp.error.system");
    }

    @Test
    void sendPayment_returnsBadRequest_whenMemberNotFound() {
        when(memberService.findById(404L)).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response =
                controller.sendPayment(404L, 50.0, "EUR", "MEMBERSHIP_FEE", null);

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(bodyOf(response).get("success")).isEqualTo(false);
    }
}
