package org.example.civitaswebapp.service;

import org.example.civitaswebapp.domain.Event;
import org.example.civitaswebapp.domain.EventType;
import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.dto.events.EventRequest;
import org.example.civitaswebapp.dto.events.EventSavedEventDto;
import org.example.civitaswebapp.repository.EventRepository;
import org.example.civitaswebapp.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    public Page<Event> getEvents(Pageable pageable) {
        return eventRepository.findAll(pageable);
    }

    @Override
    public Optional<Event> findById(Long id) {
        return eventRepository.findById(id);
    }


    @Transactional
    @Override
    public Event saveEvent(EventRequest eventRequest) {
        boolean isNew = eventRequest.getId() == null;

        Event event = isNew ? new Event() : eventRepository.findById(eventRequest.getId())
                .orElseThrow(() -> new RuntimeException("Event not found"));

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

        // Publish notification event
        EventSavedEventDto dto = new EventSavedEventDto(
                savedEvent.getId(),
                savedEvent.getTitle(),
                savedEvent.getDescription(),
                savedEvent.getStart(),
                savedEvent.getEnd(),
                eventRequest.getCreatedByUserId(), // You may need to include this in EventRequest
                isNew
        );
        eventPublisher.publishEvent(dto);

        return savedEvent;
    }


    @Override
    public void deleteEvent(Event event) {
        eventRepository.delete(event);
    }
}
