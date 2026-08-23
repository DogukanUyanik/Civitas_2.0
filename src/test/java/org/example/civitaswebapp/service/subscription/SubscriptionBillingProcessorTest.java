package org.example.civitaswebapp.service.subscription;

import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.domain.MemberSubscriptionStatus;
import org.example.civitaswebapp.domain.SubscriptionFrequency;
import org.example.civitaswebapp.domain.Transaction;
import org.example.civitaswebapp.domain.TransactionStatus;
import org.example.civitaswebapp.domain.TransactionType;
import org.example.civitaswebapp.domain.Union;
import org.example.civitaswebapp.repository.MemberRepository;
import org.example.civitaswebapp.repository.TransactionRepository;
import org.example.civitaswebapp.service.TransactionService;
import org.example.civitaswebapp.service.communication.WhatsAppService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
 * it over WhatsApp, and advance the billing clock — plus the guard against the production incident
 * where a member with a stale {@code nextBillingDate} was re-billed and re-messaged every single
 * night: an already-outstanding PENDING charge must be reused (reminded on a cooldown within its
 * grace period), never duplicated.
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionBillingProcessorTest {

    private static final int GRACE_PERIOD_DAYS = 14;
    private static final int REMINDER_COOLDOWN_DAYS = 3;

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private TransactionService transactionService;
    @Mock
    private WhatsAppService whatsAppService;

    @InjectMocks
    private SubscriptionBillingProcessor processor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(processor, "pendingGracePeriodDays", GRACE_PERIOD_DAYS);
        ReflectionTestUtils.setField(processor, "reminderCooldownDays", REMINDER_COOLDOWN_DAYS);
    }

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

    private void stubNoExistingPending(Member member) {
        when(transactionRepository.findFirstByMemberAndTypeAndStatusOrderByCreatedAtDesc(
                eq(member), eq(TransactionType.MEMBERSHIP_FEE), eq(TransactionStatus.PENDING)))
                .thenReturn(Optional.empty());
    }

    @Test
    void billsMonthlyMember_createsChargeSendsLinkAndAdvancesOneMonth() {
        LocalDate dueYesterday = LocalDate.now().minusDays(1);
        Member member = dueMember(SubscriptionFrequency.MONTHLY, dueYesterday, new BigDecimal("25.00"));
        Transaction tx = Transaction.builder().id(99L).amount(25.0).build();
        stubNoExistingPending(member);

        when(memberRepository.findById(7L)).thenReturn(Optional.of(member));
        when(transactionService.createSubscriptionTransaction(eq(member), anyDouble())).thenReturn(tx);
        when(transactionService.generateStripePaymentLink(tx)).thenReturn("https://pay.example/abc");

        processor.processDueMember(7L);

        verify(transactionService).createSubscriptionTransaction(member, 25.0);
        verify(transactionService).generateStripePaymentLink(tx);
        verify(whatsAppService).sendPaymentLink(member, "https://pay.example/abc");
        // Only one day behind, so fast-forward is a no-op — a single cadence step is enough.
        assertThat(member.getNextBillingDate()).isEqualTo(dueYesterday.plusMonths(1));
        verify(memberRepository).save(member);
    }

    @Test
    void billsYearlyMember_advancesOneYear() {
        LocalDate dueYesterday = LocalDate.now().minusDays(1);
        Member member = dueMember(SubscriptionFrequency.YEARLY, dueYesterday, new BigDecimal("120.00"));
        Transaction tx = Transaction.builder().id(100L).amount(120.0).build();
        stubNoExistingPending(member);

        when(memberRepository.findById(7L)).thenReturn(Optional.of(member));
        when(transactionService.createSubscriptionTransaction(eq(member), anyDouble())).thenReturn(tx);
        when(transactionService.generateStripePaymentLink(tx)).thenReturn("https://pay.example/xyz");

        processor.processDueMember(7L);

        assertThat(member.getNextBillingDate()).isEqualTo(dueYesterday.plusYears(1));
        verify(whatsAppService).sendPaymentLink(member, "https://pay.example/xyz");
    }

    @Test
    void memberWithoutValidAmount_isRejectedAndNothingIsCharged() {
        LocalDate dueYesterday = LocalDate.now().minusDays(1);
        Member member = dueMember(SubscriptionFrequency.MONTHLY, dueYesterday, null);
        when(memberRepository.findById(7L)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> processor.processDueMember(7L))
                .isInstanceOf(IllegalStateException.class);

        verify(transactionService, never()).createSubscriptionTransaction(any(), anyDouble());
        verify(whatsAppService, never()).sendPaymentLink(any(), any());
        verify(memberRepository, never()).save(any());
        assertThat(member.getNextBillingDate()).isEqualTo(dueYesterday); // unchanged
    }

    @Test
    void memberSeveralCyclesBehind_fastForwardsToNextFutureDateInsteadOfOneStep() {
        // 10 months behind: a naive single-step advance would still land in the past.
        LocalDate longOverdue = LocalDate.now().minusMonths(10);
        Member member = dueMember(SubscriptionFrequency.MONTHLY, longOverdue, new BigDecimal("25.00"));
        Transaction tx = Transaction.builder().id(101L).amount(25.0).build();
        stubNoExistingPending(member);

        when(memberRepository.findById(7L)).thenReturn(Optional.of(member));
        when(transactionService.createSubscriptionTransaction(eq(member), anyDouble())).thenReturn(tx);
        when(transactionService.generateStripePaymentLink(tx)).thenReturn("https://pay.example/abc");

        processor.processDueMember(7L);

        LocalDate result = member.getNextBillingDate();
        assertThat(result).isAfter(LocalDate.now());
        // It landed on the very next monthly boundary after today, not further ahead — the missed
        // cycles were forgiven, not skipped over into the future.
        assertThat(result.minusMonths(1)).isBeforeOrEqualTo(LocalDate.now());
    }

    @Test
    void memberWithFreshPendingCharge_isNotBilledAgainAndClockDoesNotAdvance() {
        LocalDate nextBilling = LocalDate.now().minusDays(1);
        Member member = dueMember(SubscriptionFrequency.MONTHLY, nextBilling, new BigDecimal("25.00"));
        Transaction existingPending = Transaction.builder()
                .id(50L).status(TransactionStatus.PENDING)
                .createdAt(LocalDateTime.now().minusDays(1)) // created last night's run
                .build();

        when(memberRepository.findById(7L)).thenReturn(Optional.of(member));
        when(transactionRepository.findFirstByMemberAndTypeAndStatusOrderByCreatedAtDesc(
                eq(member), eq(TransactionType.MEMBERSHIP_FEE), eq(TransactionStatus.PENDING)))
                .thenReturn(Optional.of(existingPending));

        processor.processDueMember(7L);

        verify(transactionService, never()).createSubscriptionTransaction(any(), anyDouble());
        verify(memberRepository, never()).save(any());
        assertThat(member.getNextBillingDate()).isEqualTo(nextBilling); // untouched
        // Within the reminder cooldown (created only 1 day ago, cooldown is 3 days) — no reminder either.
        verify(whatsAppService, never()).sendPaymentLink(any(), any());
    }

    @Test
    void memberWithStalePendingCharge_getsRemindedAfterCooldownWithoutDuplicateCharge() {
        Member member = dueMember(SubscriptionFrequency.MONTHLY, LocalDate.now().minusDays(1), new BigDecimal("25.00"));
        Transaction existingPending = Transaction.builder()
                .id(50L).status(TransactionStatus.PENDING)
                .createdAt(LocalDateTime.now().minusDays(REMINDER_COOLDOWN_DAYS + 1))
                .build();

        when(memberRepository.findById(7L)).thenReturn(Optional.of(member));
        when(transactionRepository.findFirstByMemberAndTypeAndStatusOrderByCreatedAtDesc(
                eq(member), eq(TransactionType.MEMBERSHIP_FEE), eq(TransactionStatus.PENDING)))
                .thenReturn(Optional.of(existingPending));
        when(transactionService.generateStripePaymentLink(existingPending)).thenReturn("https://pay.example/reminder");

        processor.processDueMember(7L);

        // Reminded using the SAME transaction — no new charge created.
        verify(transactionService, never()).createSubscriptionTransaction(any(), anyDouble());
        verify(transactionService).generateStripePaymentLink(existingPending);
        verify(whatsAppService).sendPaymentLink(member, "https://pay.example/reminder");
        assertThat(existingPending.getLastReminderSentAt()).isNotNull();
        verify(transactionRepository).save(existingPending);
    }

    @Test
    void memberWithExpiredPendingCharge_getsRebilledForANewCycle() {
        Member member = dueMember(SubscriptionFrequency.MONTHLY, LocalDate.now().minusDays(1), new BigDecimal("25.00"));
        Transaction stalePending = Transaction.builder()
                .id(50L).status(TransactionStatus.PENDING)
                .createdAt(LocalDateTime.now().minusDays(GRACE_PERIOD_DAYS + 1))
                .build();
        Transaction newTx = Transaction.builder().id(102L).amount(25.0).build();

        when(memberRepository.findById(7L)).thenReturn(Optional.of(member));
        when(transactionRepository.findFirstByMemberAndTypeAndStatusOrderByCreatedAtDesc(
                eq(member), eq(TransactionType.MEMBERSHIP_FEE), eq(TransactionStatus.PENDING)))
                .thenReturn(Optional.of(stalePending));
        when(transactionService.createSubscriptionTransaction(eq(member), anyDouble())).thenReturn(newTx);
        when(transactionService.generateStripePaymentLink(newTx)).thenReturn("https://pay.example/new");

        processor.processDueMember(7L);

        assertThat(stalePending.getStatus()).isEqualTo(TransactionStatus.EXPIRED);
        verify(transactionRepository).save(stalePending);
        verify(transactionService).createSubscriptionTransaction(member, 25.0);
        verify(whatsAppService).sendPaymentLink(member, "https://pay.example/new");
        verify(memberRepository).save(member);
    }
}
