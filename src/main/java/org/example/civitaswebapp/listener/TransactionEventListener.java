package org.example.civitaswebapp.listener;

import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.domain.NotificationType;
import org.example.civitaswebapp.domain.TransactionStatus;
import org.example.civitaswebapp.dto.transactions.TransactionCreatedDto;
import org.example.civitaswebapp.dto.transactions.TransactionStatusChangedDto;
import org.example.civitaswebapp.repository.MemberRepository;
import org.example.civitaswebapp.repository.MyUserRepository;
import org.example.civitaswebapp.service.communication.NotificationService;
import org.example.civitaswebapp.service.communication.WhatsAppService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
public class TransactionEventListener {

    private final NotificationService notificationService;
    private final MyUserRepository myUserRepository;
    private final MemberRepository memberRepository;
    private final WhatsAppService whatsAppService;

    public TransactionEventListener(NotificationService notificationService, MyUserRepository myUserRepository,
                                    MemberRepository memberRepository, WhatsAppService whatsAppService) {
        this.notificationService = notificationService;
        this.myUserRepository = myUserRepository;
        this.memberRepository = memberRepository;
        this.whatsAppService = whatsAppService;
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
                            "notification.transaction.created.title",
                            "notification.transaction.created.message",
                            List.of(dto.firstName() + " " + dto.lastName()),
                            NotificationType.TRANSACTION,
                            "/transactions"
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
                        "notification.transaction.statusChanged.title",
                        "notification.transaction.statusChanged.message",
                        List.of(String.valueOf(dto.id()), String.valueOf(dto.newStatus())),
                        NotificationType.TRANSACTION,
                        "/transactions"
                );
            }
        } catch (Exception e) {
            System.err.println("⚠️ Status notification failed: " + e.getMessage());
        }

        try {
            if (dto.newStatus() == TransactionStatus.SUCCEEDED || dto.newStatus() == TransactionStatus.FAILED) {
                Member member = memberRepository.findById(dto.memberId()).orElse(null);
                if (member != null) {
                    if (dto.newStatus() == TransactionStatus.SUCCEEDED) {
                        whatsAppService.sendPaymentSuccess(member);
                    } else {
                        whatsAppService.sendPaymentFailed(member);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ WhatsApp payment notification failed: " + e.getMessage());
        }
    }
}