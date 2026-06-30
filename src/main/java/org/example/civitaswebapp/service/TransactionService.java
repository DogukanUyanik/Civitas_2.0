package org.example.civitaswebapp.service;

import org.example.civitaswebapp.domain.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TransactionService {
    Transaction createTransaction(Member member, double amount, TransactionType type, MyUser createdByUser);

    /**
     * Creates a PENDING membership-fee transaction in a system context (no logged-in user) — used
     * by the automated subscription scheduler. The union is stamped from the member's own union,
     * and the transaction is flagged as an automated subscription payment.
     */
    Transaction createSubscriptionTransaction(Member member, double amount);
    String generateStripePaymentLink(Transaction transaction);
    void handleStripeWebhook(String payload, String sigHeader);
    List<Transaction> getTransactionsByMember(Member member);
    void updateTransactionStatus(Long transactionId, TransactionStatus status);
    void updateTransactionStatusAsSystem(Long transactionId, TransactionStatus status);

    /**
     * Settles a PENDING transaction as a manual/cash payment without going through Stripe:
     * union-scoped, flips the status to PAID_MANUALLY instantly, records a note and the member's
     * last-payment date, and publishes the usual status-changed event. PAID_MANUALLY counts as
     * revenue but stays distinct from Stripe online payments for accounting.
     */
    void markTransactionAsCash(Long transactionId);

    Page<Transaction> getTransactions(Pageable pageable, String search, TransactionStatus status, TransactionType type);

}
