package org.example.civitaswebapp.service.subscription;

import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.domain.MemberSubscriptionStatus;
import org.example.civitaswebapp.domain.SubscriptionFrequency;
import org.example.civitaswebapp.dto.subscription.SubscriptionBillingResult;
import org.example.civitaswebapp.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
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
}
