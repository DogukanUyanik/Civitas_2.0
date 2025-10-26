package org.example.civitaswebapp.listener;

import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.domain.NotificationType;
import org.example.civitaswebapp.dto.events.EventSavedEventDto;
import org.example.civitaswebapp.repository.MyUserRepository;
import org.example.civitaswebapp.service.NotificationService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class EventNotificationListener {

    private final NotificationService notificationService;
    private final MyUserRepository myUserRepository;

    public EventNotificationListener(NotificationService notificationService,
                                     MyUserRepository myUserRepository) {
        this.notificationService = notificationService;
        this.myUserRepository = myUserRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleEventSaved(EventSavedEventDto dto) {
        MyUser user = myUserRepository.findById(dto.createdByUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String action = dto.isNew() ? "aangemaakt" : "aangepast";
        String message = "Event " + dto.title() + " om " + dto.start() + " tot " + dto.end() + " " + action;

        notificationService.createNotification(
                user,
                dto.isNew() ? "Nieuw event" : "Event aangepast",
                message,
                NotificationType.EVENT,
                "/events/" + dto.eventId()
        );
    }
}

