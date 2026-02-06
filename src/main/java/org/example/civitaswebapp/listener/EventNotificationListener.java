package org.example.civitaswebapp.listener;

import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.domain.NotificationType;
import org.example.civitaswebapp.dto.events.EventSavedEventDto;
import org.example.civitaswebapp.repository.EventRepository;
import org.example.civitaswebapp.repository.MyUserRepository;
import org.example.civitaswebapp.service.communication.NotificationService;
import org.example.civitaswebapp.service.communication.WhatsAppService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class EventNotificationListener {

    private final NotificationService notificationService;
    private final MyUserRepository myUserRepository;
    private final WhatsAppService whatsAppService;
    private final EventRepository eventRepository;

    public EventNotificationListener(NotificationService notificationService,
                                     MyUserRepository myUserRepository,
                                     WhatsAppService whatsAppService,
                                     EventRepository eventRepository) {
        this.notificationService = notificationService;
        this.myUserRepository = myUserRepository;
        this.whatsAppService = whatsAppService;
        this.eventRepository = eventRepository;
    }

    @Async
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

        eventRepository.findById(dto.eventId()).ifPresent(event -> {

            if (event.getAttendees() != null && !event.getAttendees().isEmpty()) {

                for (var member : event.getAttendees()) {
                    try {
                        whatsAppService.sendEventNotification(member.getPhoneNumber(), event);
                    } catch (Exception e) {
                        System.err.println("Failed to WhatsApp member " + member.getId() + ": " + e.getMessage());
                    }
                }
            }
        });
    }
}