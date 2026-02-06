package org.example.civitaswebapp.util;

import org.example.civitaswebapp.domain.Event;
import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.repository.EventRepository;
import org.example.civitaswebapp.service.communication.WhatsAppService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Component
public class EventReminderTask {

    private final EventRepository eventRepository;
    private final WhatsAppService whatsAppService;

    public EventReminderTask(EventRepository eventRepository, WhatsAppService whatsAppService){
        this.eventRepository = eventRepository;
        this.whatsAppService = whatsAppService;
    }


    @Scheduled(cron = "0 0 10 * * *")
    @Transactional
    public void sendEventReminders(){
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrowStart = now.plusDays(1);
        LocalDateTime tomorrowEnd = now.plusDays(2);

        List<Event> upcomingEvents = eventRepository.findByStartBetween(tomorrowStart, tomorrowEnd);

        for (Event event : upcomingEvents){
            Set<Member> attendees = event.getAttendees();

            if (attendees == null || attendees.isEmpty()) continue;

            for (Member member : attendees){
                whatsAppService.sendEventNotification(member.getPhoneNumber(), event);
            }
        }
    }
}
