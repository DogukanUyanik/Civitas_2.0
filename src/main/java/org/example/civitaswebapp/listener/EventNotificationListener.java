package org.example.civitaswebapp.listener;

import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.domain.NotificationType;
import org.example.civitaswebapp.dto.events.EventMessageDetails;
import org.example.civitaswebapp.dto.events.EventSavedEventDto;
import org.example.civitaswebapp.repository.MyUserRepository;
import org.example.civitaswebapp.service.communication.NotificationService;
import org.example.civitaswebapp.service.communication.WhatsAppService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class EventNotificationListener {

    private final NotificationService notificationService;
    private final MyUserRepository myUserRepository;
    private final WhatsAppService whatsAppService;

    public EventNotificationListener(NotificationService notificationService,
                                     MyUserRepository myUserRepository,
                                     WhatsAppService whatsAppService) {
        this.notificationService = notificationService;
        this.myUserRepository = myUserRepository;
        this.whatsAppService = whatsAppService;
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
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

        // Work purely from the immutable DTO — never reload the Event or touch its attendees
        // PersistentSet. This keeps the async thread fully isolated from the request thread's
        // Hibernate session (no shared collection-loading state -> no ConcurrentModificationException).
        EventMessageDetails messageDetails = new EventMessageDetails(
                dto.title(),
                dto.eventType(),
                dto.start(),
                dto.end(),
                dto.location(),
                dto.description()
        );

        for (String phoneNumber : dto.attendeePhoneNumbers()) {
            try {
                whatsAppService.sendEventNotification(phoneNumber, messageDetails);
            } catch (Exception e) {
                System.err.println("Failed to WhatsApp " + phoneNumber + ": " + e.getMessage());
            }
        }
    }
}
