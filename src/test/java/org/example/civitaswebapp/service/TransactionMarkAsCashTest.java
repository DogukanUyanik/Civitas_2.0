package org.example.civitaswebapp.service;

import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.domain.Transaction;
import org.example.civitaswebapp.domain.TransactionStatus;
import org.example.civitaswebapp.domain.TransactionType;
import org.example.civitaswebapp.domain.Union;
import org.example.civitaswebapp.dto.transactions.TransactionStatusChangedDto;
import org.example.civitaswebapp.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Guards the manual/cash settlement path of {@code TransactionServiceImpl.markTransactionAsCash}:
 * union-scoped, only settles PENDING transactions, flips to SUCCEEDED instantly (no Stripe), and
 * publishes the same status-changed event the webhook flow uses.
 */
@ExtendWith(MockitoExtension.class)
class TransactionMarkAsCashTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private MyUserService myUserService;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private final Union union = Union.builder().id(UUID.randomUUID()).name("Demo").build();

    private void stubLoggedInUser() {
        when(myUserService.getLoggedInUser()).thenReturn(MyUser.builder().id(1L).union(union).build());
    }

    private Transaction pendingTransaction() {
        Member member = Member.builder().id(2L).firstName("Ada").lastName("Lovelace").union(union).build();
        return Transaction.builder()
                .id(5L)
                .amount(25.0)
                .currency("EUR")
                .type(TransactionType.MEMBERSHIP_FEE)
                .status(TransactionStatus.PENDING)
                .member(member)
                .union(union)
                .build();
    }

    @Test
    void marksPendingTransactionAsPaidManually() {
        stubLoggedInUser();
        Transaction tx = pendingTransaction();
        when(transactionRepository.findByIdAndUnion(eq(5L), any())).thenReturn(Optional.of(tx));

        transactionService.markTransactionAsCash(5L);

        // Distinct from Stripe SUCCEEDED so the treasurer can separate cash from online revenue.
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.PAID_MANUALLY);
        assertThat(tx.getNote()).containsIgnoringCase("cash");
        assertThat(tx.getMember().getDateOfLastPayment()).isEqualTo(LocalDate.now());
        verify(transactionRepository).save(tx);
        verify(eventPublisher).publishEvent(any(TransactionStatusChangedDto.class));
    }

    @Test
    void paidManuallyCountsAsRevenueAlongsideSucceeded() {
        // Guards the treasurer requirement: cash is revenue, but tracked separately from online.
        assertThat(TransactionStatus.revenueStatuses())
                .containsExactlyInAnyOrder(TransactionStatus.SUCCEEDED, TransactionStatus.PAID_MANUALLY)
                .doesNotContain(TransactionStatus.PENDING, TransactionStatus.FAILED, TransactionStatus.EXPIRED);
    }

    @Test
    void rejectsNonPendingTransaction() {
        stubLoggedInUser();
        Transaction tx = pendingTransaction();
        tx.setStatus(TransactionStatus.SUCCEEDED);
        when(transactionRepository.findByIdAndUnion(eq(5L), any())).thenReturn(Optional.of(tx));

        assertThatThrownBy(() -> transactionService.markTransactionAsCash(5L))
                .isInstanceOf(IllegalStateException.class);

        verify(transactionRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void throwsWhenTransactionNotFoundInUnion() {
        stubLoggedInUser();
        when(transactionRepository.findByIdAndUnion(eq(99L), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.markTransactionAsCash(99L))
                .isInstanceOf(RuntimeException.class);

        verify(transactionRepository, never()).save(any());
    }
}
