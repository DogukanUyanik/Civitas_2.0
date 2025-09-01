package org.example.civitaswebapp.service;

import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.domain.Transaction;
import org.example.civitaswebapp.domain.TransactionStatus;
import org.example.civitaswebapp.domain.TransactionType;

import java.util.List;

public interface TransactionService {
    Transaction createTransaction(Member member, double amount, TransactionType type);
    String generateStripePaymentLink(Transaction transaction);
    void handleStripeWebhook(String payload, String sigHeader);
    List<Transaction> getTransactionsByMember(Member member);
    void updateTransactionStatus(Long transactionId, TransactionStatus status);

}
