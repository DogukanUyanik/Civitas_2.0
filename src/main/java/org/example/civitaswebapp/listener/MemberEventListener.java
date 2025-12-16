package org.example.civitaswebapp.listener;

import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.domain.NotificationType;
import org.example.civitaswebapp.dto.member.MemberSavedEventDto;
import org.example.civitaswebapp.repository.MyUserRepository;
import org.example.civitaswebapp.service.NotificationService;
import org.springframework.context.event.EventListener;
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

        if (dto.isNew()) {
            notificationService.createNotification(
                    user,
                    "Nieuw lid toegevoegd",
                    "Lid " + dto.firstName() + " " + dto.lastName() + " is toegevoegd.",
                    NotificationType.MEMBER,
                    "/members/view/" + dto.memberId()
            );
        } else {
            notificationService.createNotification(
                    user,
                    "Lid geupdate",
                    "Lid " + dto.firstName() + " " + dto.lastName() + " is geupdate",
                    NotificationType.MEMBER,
                    "/members/view/" + dto.memberId()
            );
        }
    }
}

