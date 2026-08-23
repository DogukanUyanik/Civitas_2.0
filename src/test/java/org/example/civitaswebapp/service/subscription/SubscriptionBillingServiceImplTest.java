package org.example.civitaswebapp.service.subscription;

import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.domain.MemberSubscriptionStatus;
import org.example.civitaswebapp.domain.SubscriptionFrequency;
import org.example.civitaswebapp.domain.Transaction;
import org.example.civitaswebapp.domain.TransactionStatus;
import org.example.civitaswebapp.domain.TransactionType;
import org.example.civitaswebapp.dto.subscription.SubscriptionBillingResult;
import org.example.civitaswebapp.repository.MemberRepository;
import org.example.civitaswebapp.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Guards the core resilience requirement of the billing engine: one member's failure (bad phone,
 * Stripe error, etc.) must be isolated and logged, never aborting the run for the others.
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionBillingServiceImplTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private SubscriptionBillingProcessor processor;

    @InjectMocks
    private SubscriptionBillingServiceImpl billingService;

    private Member member(long id) {
        return Member.builder().id(id).firstName("Member").lastName(String.valueOf(id)).build();
    }

    @Test
    void queriesActiveDueSubscribersExcludingNoneFrequency() {
        when(memberRepository.findDueSubscriptions(any(), any(), any())).thenReturn(List.of());

        billingService.runDueSubscriptions();

        verify(memberRepository).findDueSubscriptions(
                eq(MemberSubscriptionStatus.ACTIVE), eq(SubscriptionFrequency.NONE), any(LocalDate.class));
    }

    @Test
    void billsEveryDueMember() {
        when(memberRepository.findDueSubscriptions(any(), any(), any()))
                .thenReturn(List.of(member(1), member(2), member(3)));

        SubscriptionBillingResult result = billingService.runDueSubscriptions();

        verify(processor).processDueMember(1L);
        verify(processor).processDueMember(2L);
        verify(processor).processDueMember(3L);
        assertThat(result.due()).isEqualTo(3);
        assertThat(result.succeeded()).isEqualTo(3);
        assertThat(result.failed()).isZero();
    }

    @Test
    void oneFailingMemberDoesNotAbortTheRun() {
        when(memberRepository.findDueSubscriptions(any(), any(), any()))
                .thenReturn(List.of(member(1), member(2), member(3)));
        doNothing().when(processor).processDueMember(1L);
        doThrow(new RuntimeException("invalid phone number")).when(processor).processDueMember(2L);
        doNothing().when(processor).processDueMember(3L);

        SubscriptionBillingResult result = billingService.runDueSubscriptions();

        // The failing member (2) is skipped, but 1 and 3 are still billed.
        verify(processor).processDueMember(1L);
        verify(processor).processDueMember(2L);
        verify(processor).processDueMember(3L);
        assertThat(result.due()).isEqualTo(3);
        assertThat(result.succeeded()).isEqualTo(2);
        assertThat(result.failed()).isEqualTo(1);
    }

    private Transaction pendingTx(long id, Member member, LocalDateTime createdAt) {
        return Transaction.builder()
                .id(id).member(member).status(TransactionStatus.PENDING).createdAt(createdAt)
                .build();
    }

    @Test
    void cleanup_expiresAllButTheMostRecentPendingChargePerMember() {
        Member ada = member(1);
        Member bob = member(2);
        // Repository contract: ordered by member id asc, createdAt desc within each member.
        Transaction adaNewest = pendingTx(10L, ada, LocalDateTime.now().minusDays(1));
        Transaction adaOlder1 = pendingTx(11L, ada, LocalDateTime.now().minusDays(5));
        Transaction adaOlder2 = pendingTx(12L, ada, LocalDateTime.now().minusDays(9));
        Transaction bobOnly = pendingTx(20L, bob, LocalDateTime.now().minusDays(2));

        when(transactionRepository.findAllByTypeAndStatusOrderByMemberIdAscCreatedAtDesc(
                eq(TransactionType.MEMBERSHIP_FEE), eq(TransactionStatus.PENDING)))
                .thenReturn(List.of(adaNewest, adaOlder1, adaOlder2, bobOnly));

        int expired = billingService.cleanupDuplicatePendingSubscriptionTransactions();

        assertThat(expired).isEqualTo(2);
        assertThat(adaNewest.getStatus()).isEqualTo(TransactionStatus.PENDING); // kept
        assertThat(adaOlder1.getStatus()).isEqualTo(TransactionStatus.EXPIRED);
        assertThat(adaOlder2.getStatus()).isEqualTo(TransactionStatus.EXPIRED);
        assertThat(bobOnly.getStatus()).isEqualTo(TransactionStatus.PENDING); // only one, untouched
        verify(transactionRepository).save(adaOlder1);
        verify(transactionRepository).save(adaOlder2);
        verify(transactionRepository, never()).save(adaNewest);
        verify(transactionRepository, never()).save(bobOnly);
    }

    @Test
    void cleanup_isNoOpWhenNoMemberHasDuplicates() {
        Member ada = member(1);
        when(transactionRepository.findAllByTypeAndStatusOrderByMemberIdAscCreatedAtDesc(any(), any()))
                .thenReturn(List.of(pendingTx(10L, ada, LocalDateTime.now())));

        int expired = billingService.cleanupDuplicatePendingSubscriptionTransactions();

        assertThat(expired).isZero();
        verify(transactionRepository, never()).save(any());
    }
}
