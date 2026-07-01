package org.example.civitaswebapp.service.subscription;

import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.domain.MemberSubscriptionStatus;
import org.example.civitaswebapp.domain.SubscriptionFrequency;
import org.example.civitaswebapp.domain.Transaction;
import org.example.civitaswebapp.domain.Union;
import org.example.civitaswebapp.repository.MemberRepository;
import org.example.civitaswebapp.service.TransactionService;
import org.example.civitaswebapp.service.communication.WhatsAppService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the per-member billing steps: persist a PENDING charge, generate the Stripe link, send
 * it over WhatsApp, and advance the billing clock from the current nextBillingDate.
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionBillingProcessorTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private TransactionService transactionService;
    @Mock
    private WhatsAppService whatsAppService;

    @InjectMocks
    private SubscriptionBillingProcessor processor;

    private Member dueMember(SubscriptionFrequency frequency, LocalDate nextBilling, BigDecimal amount) {
        Union union = Union.builder().id(UUID.randomUUID()).name("Demo").build();
        return Member.builder()
                .id(7L)
                .firstName("Ada").lastName("Lovelace")
                .phoneNumber("+32470123456")
                .subscriptionFrequency(frequency)
                .subscriptionStatus(MemberSubscriptionStatus.ACTIVE)
                .subscriptionAmount(amount)
                .nextBillingDate(nextBilling)
                .union(union)
                .build();
    }

    @Test
    void billsMonthlyMember_createsChargeSendsLinkAndAdvancesOneMonth() {
        Member member = dueMember(SubscriptionFrequency.MONTHLY, LocalDate.of(2026, 6, 30), new BigDecimal("25.00"));
        Transaction tx = Transaction.builder().id(99L).amount(25.0).build();

        when(memberRepository.findById(7L)).thenReturn(Optional.of(member));
        when(transactionService.createSubscriptionTransaction(eq(member), anyDouble())).thenReturn(tx);
        when(transactionService.generateStripePaymentLink(tx)).thenReturn("https://pay.example/abc");

        processor.processDueMember(7L);

        verify(transactionService).createSubscriptionTransaction(member, 25.0);
        verify(transactionService).generateStripePaymentLink(tx);
        verify(whatsAppService).sendPaymentLink("+32470123456", "https://pay.example/abc");
        assertThat(member.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 7, 30));
        verify(memberRepository).save(member);
    }

    @Test
    void billsYearlyMember_advancesOneYear() {
        Member member = dueMember(SubscriptionFrequency.YEARLY, LocalDate.of(2026, 6, 30), new BigDecimal("120.00"));
        Transaction tx = Transaction.builder().id(100L).amount(120.0).build();

        when(memberRepository.findById(7L)).thenReturn(Optional.of(member));
        when(transactionService.createSubscriptionTransaction(eq(member), anyDouble())).thenReturn(tx);
        when(transactionService.generateStripePaymentLink(tx)).thenReturn("https://pay.example/xyz");

        processor.processDueMember(7L);

        assertThat(member.getNextBillingDate()).isEqualTo(LocalDate.of(2027, 6, 30));
        verify(whatsAppService).sendPaymentLink("+32470123456", "https://pay.example/xyz");
    }

    @Test
    void memberWithoutValidAmount_isRejectedAndNothingIsCharged() {
        Member member = dueMember(SubscriptionFrequency.MONTHLY, LocalDate.of(2026, 6, 30), null);
        when(memberRepository.findById(7L)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> processor.processDueMember(7L))
                .isInstanceOf(IllegalStateException.class);

        verify(transactionService, never()).createSubscriptionTransaction(any(), anyDouble());
        verify(whatsAppService, never()).sendPaymentLink(any(), any());
        verify(memberRepository, never()).save(any());
        assertThat(member.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 6, 30)); // unchanged
    }
}
