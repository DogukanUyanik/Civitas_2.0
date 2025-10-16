package org.example.civitaswebapp.listener;

import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.domain.Notification;
import org.example.civitaswebapp.domain.NotificationStatus;
import org.example.civitaswebapp.domain.NotificationType;
import org.example.civitaswebapp.dto.member.MemberCreatedEventDto;
import org.example.civitaswebapp.repository.MyUserRepository;
import org.example.civitaswebapp.repository.NotificationRepository;
import org.example.civitaswebapp.service.NotificationService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
@Component
public class MemberEventListener {

    private final NotificationRepository notificationRepository;
    private final MyUserRepository myUserRepository;

    public MemberEventListener(NotificationRepository notificationRepository,
                               MyUserRepository myUserRepository) {
        this.notificationRepository = notificationRepository;
        this.myUserRepository = myUserRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleMemberCreated(MemberCreatedEventDto dto) {
        System.out.println("📢 MemberEventListener triggered for member: " + dto.getFirstName() + " " + dto.getLastName());

        MyUser user = myUserRepository.findById(dto.getCreatedByUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Notification notification = Notification.builder()
                .user(user)
                .title("Nieuw lid toegevoegd")
                .message("Lid " + dto.getFirstName() + " " + dto.getLastName() + " is toegevoegd.")
                .type(NotificationType.MEMBER)
                .status(NotificationStatus.UNREAD)
                .createdAt(Instant.now())
                .build();

        // Force flush immediately to ensure ID is generated
        notificationRepository.saveAndFlush(notification);

        System.out.println("📢 Notification saved with ID: " + notification.getId());
    }
}
