package org.example.civitaswebapp.service;

import org.example.civitaswebapp.domain.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TransactionService {
    Transaction createTransaction(Member member, double amount, TransactionType type, MyUser createdByUser);
    String generateStripePaymentLink(Transaction transaction);
    void handleStripeWebhook(String payload, String sigHeader);
    List<Transaction> getTransactionsByMember(Member member);
    void updateTransactionStatus(Long transactionId, TransactionStatus status);

    Page<Transaction> getTransactions(Pageable pageable, String search, TransactionStatus status, TransactionType type);

}
