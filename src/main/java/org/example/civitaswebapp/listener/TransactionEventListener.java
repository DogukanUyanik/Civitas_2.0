package org.example.civitaswebapp.listener;

import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.domain.NotificationType;
import org.example.civitaswebapp.dto.transactions.TransactionCreatedDto;
import org.example.civitaswebapp.dto.transactions.TransactionStatusChangedDto;
import org.example.civitaswebapp.repository.MyUserRepository;
import org.example.civitaswebapp.repository.TransactionRepository;
import org.example.civitaswebapp.service.NotificationService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class TransactionEventListener {

    private final NotificationService notificationService;
    private final MyUserRepository myUserRepository;

    public TransactionEventListener(NotificationService notificationService, TransactionRepository transactionRepository, MyUserRepository myUserRepository) {
        this.notificationService = notificationService;
        this.myUserRepository = myUserRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleTransactionCreated(TransactionCreatedDto dto){
        try {

            MyUser user = myUserRepository.findById(dto.memberId())
                    .orElseThrow(() -> new RuntimeException("User not found for notification"));

            notificationService.createNotification(
                    user,
                    "New Transaction created",
                    "New Transaction created for user " + dto.firstName() + " " + dto.lastName(),
                    NotificationType.TRANSACTION,
                    "/transactions/"
            );
        } catch (Exception e) {
            // ✅ FIX 2: Catch the error so the Payment Transaction doesn't roll back
            System.err.println("⚠️ Notification failed, but transaction will proceed: " + e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTransactionStatusChanged(TransactionStatusChangedDto dto){
        try {
            MyUser user = myUserRepository.findById(dto.memberId())
                    .orElseThrow(() -> new RuntimeException("User not found for notification"));

            notificationService.createNotification(
                    user,
                    "Transaction status updated",
                    "Transaction with id " + dto.id() + " changed from status " + dto.oldStatus() + " to " + dto.newStatus(),
                    NotificationType.TRANSACTION,
                    "/transactions/"
            );
        } catch (Exception e) {
            System.err.println("⚠️ Status notification failed: " + e.getMessage());
        }
    }
}