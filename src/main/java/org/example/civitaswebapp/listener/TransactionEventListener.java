package org.example.civitaswebapp.listener;

import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.domain.NotificationType;
import org.example.civitaswebapp.dto.transactions.TransactionCreatedDto;
import org.example.civitaswebapp.dto.transactions.TransactionStatusChangedDto;
import org.example.civitaswebapp.repository.MyUserRepository;
import org.example.civitaswebapp.service.communication.NotificationService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
public class TransactionEventListener {

    private final NotificationService notificationService;
    private final MyUserRepository myUserRepository;

    public TransactionEventListener(NotificationService notificationService, MyUserRepository myUserRepository) {
        this.notificationService = notificationService;
        this.myUserRepository = myUserRepository;
    }

    // ✅ SCENARIO 1: Manual Creation (Notify only the creator)
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleTransactionCreated(TransactionCreatedDto dto){
        try {
            if (dto.createdByUserId() != null) {
                // Only notify the specific admin who created this
                myUserRepository.findById(dto.createdByUserId()).ifPresent(user -> {
                    notificationService.createNotification(
                            user,
                            "Transaction Created",
                            "Generated a payment link for " + dto.firstName() + " " + dto.lastName(),
                            NotificationType.TRANSACTION,
                            "/transactions/"
                    );
                });
            }
        } catch (Exception e) {
            System.err.println("⚠️ Creation notification failed: " + e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTransactionStatusChanged(TransactionStatusChangedDto dto){
        try {
            List<MyUser> allAdmins = myUserRepository.findAll();

            for (MyUser admin : allAdmins) {
                notificationService.createNotification(
                        admin,
                        "Transaction Update",
                        "Transaction " + dto.id() + " is now " + dto.newStatus(),
                        NotificationType.TRANSACTION,
                        "/transactions/"
                );
            }
        } catch (Exception e) {
            System.err.println("⚠️ Status notification failed: " + e.getMessage());
        }
    }
}