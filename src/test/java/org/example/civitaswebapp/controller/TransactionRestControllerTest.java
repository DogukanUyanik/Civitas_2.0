package org.example.civitaswebapp.controller;

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
import org.springframework.http.ResponseEntity;

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

    @InjectMocks
    private TransactionRestController controller;

    private void stubHappyTransactionPath(String phoneNumber) {
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
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> bodyOf(ResponseEntity<Map<String, Object>> response) {
        return response.getBody();
    }

    @Test
    void sendPayment_reportsWhatsappSuccess_whenDeliverySucceeds() {
        stubHappyTransactionPath("+32470123456");

        ResponseEntity<Map<String, Object>> response =
                controller.sendPayment(1L, 50.0, "EUR", "MEMBERSHIP_FEE", null);

        Map<String, Object> body = bodyOf(response);
        assertThat(body.get("success")).isEqualTo(true);
        assertThat(body.get("whatsappSuccess")).isEqualTo(true);
        verify(whatsAppService).sendPaymentLink("+32470123456", "https://pay.example/abc");
    }

    @Test
    void sendPayment_reportsWhatsappFailure_whenTwilioThrows() {
        stubHappyTransactionPath("0470123456");
        doThrow(new IllegalArgumentException("Invalid phone number format"))
                .when(whatsAppService).sendPaymentLink(anyString(), anyString());

        ResponseEntity<Map<String, Object>> response =
                controller.sendPayment(1L, 50.0, "EUR", "MEMBERSHIP_FEE", null);

        Map<String, Object> body = bodyOf(response);
        // The transaction was still created, but delivery must NOT be reported as successful.
        assertThat(body.get("success")).isEqualTo(true);
        assertThat(body.get("whatsappSuccess")).isEqualTo(false);
        assertThat(body.get("whatsapp_error")).asString().contains("could not be sent");
    }

    @Test
    void sendPayment_reportsWhatsappFailure_whenMemberHasNoPhone() {
        stubHappyTransactionPath(null);

        ResponseEntity<Map<String, Object>> response =
                controller.sendPayment(1L, 50.0, "EUR", "MEMBERSHIP_FEE", null);

        Map<String, Object> body = bodyOf(response);
        assertThat(body.get("success")).isEqualTo(true);
        assertThat(body.get("whatsappSuccess")).isEqualTo(false);
        verify(whatsAppService, never()).sendPaymentLink(any(), any());
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
