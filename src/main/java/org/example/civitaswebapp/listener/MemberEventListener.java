package org.example.civitaswebapp.listener;

import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.domain.NotificationType;
import org.example.civitaswebapp.dto.member.MemberSavedEventDto;
import org.example.civitaswebapp.repository.MyUserRepository;
import org.example.civitaswebapp.service.communication.NotificationService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class MemberEventListener {

    private final NotificationService notificationService;
    private final MyUserRepository myUserRepository;

    public MemberEventListener(NotificationService notificationService, MyUserRepository myUserRepository) {
        this.notificationService = notificationService;
        this.myUserRepository = myUserRepository;
    }
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleMemberSaved(MemberSavedEventDto dto) {
        MyUser user = myUserRepository.findById(dto.createdByUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String fullName = dto.firstName() + " " + dto.lastName();
        if (dto.isNew()) {
            notificationService.createNotification(
                    user,
                    "notification.member.created.title",
                    "notification.member.created.message",
                    java.util.List.of(fullName),
                    NotificationType.MEMBER,
                    "/members/view/" + dto.memberId()
            );
        } else {
            notificationService.createNotification(
                    user,
                    "notification.member.updated.title",
                    "notification.member.updated.message",
                    java.util.List.of(fullName),
                    NotificationType.MEMBER,
                    "/members/view/" + dto.memberId()
            );
        }
    }
}

