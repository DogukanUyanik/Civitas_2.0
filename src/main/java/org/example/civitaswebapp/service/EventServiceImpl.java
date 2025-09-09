package org.example.civitaswebapp.service;

import org.example.civitaswebapp.domain.Event;
import org.example.civitaswebapp.domain.EventType;
import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.dto.EventRequest;
import org.example.civitaswebapp.repository.EventRepository;
import org.example.civitaswebapp.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class EventServiceImpl implements EventService {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private WhatsAppService whatsAppService;

    @Override
    public Page<Event> getEvents(Pageable pageable) {
        return eventRepository.findAll(pageable);
    }

    @Override
    public Optional<Event> findById(Long id) {
        return eventRepository.findById(id);
    }


    @Override
    public Event saveEvent(EventRequest eventRequest) {
        Event event = new Event();
        event.setTitle(eventRequest.getTitle());
        event.setDescription(eventRequest.getDescription());
        event.setStart(eventRequest.getStart());
        event.setEnd(eventRequest.getEnd());
        event.setLocation(eventRequest.getLocation());
        event.setEventType(eventRequest.getEventType() != null ? eventRequest.getEventType() : EventType.GENERAL);

        // Load members from IDs
        Set<Member> attendees = new HashSet<>(memberRepository.findAllById(eventRequest.getAttendees()));
        event.setAttendees(attendees);

        Event savedEvent = eventRepository.save(event);

        // Send WhatsApp messages
        for (Member member : attendees) {
            whatsAppService.sendEventNotification(member.getPhoneNumber(), savedEvent);
        }

        return savedEvent;
    }

    @Override
    public void deleteEvent(Event event) {
        eventRepository.delete(event);
    }
}
