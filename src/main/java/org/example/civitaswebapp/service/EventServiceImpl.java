package org.example.civitaswebapp.service;

import org.example.civitaswebapp.domain.*;
import org.example.civitaswebapp.dto.events.EventRequest;
import org.example.civitaswebapp.dto.events.EventSavedEventDto;
import org.example.civitaswebapp.repository.EventRepository;
import org.example.civitaswebapp.repository.MemberRepository;
import org.example.civitaswebapp.service.communication.WhatsAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
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
        return eventRepository.findAllByUnion(getCurrentUserUnion(), pageable);
    }

    @Override
    public Optional<Event> findById(Long id) {
        return eventRepository.findByIdAndUnion(id, getCurrentUserUnion());
    }



    @Transactional
    @Override
    public Event saveEvent(EventRequest eventRequest, MyUser user) {
        boolean isNew = eventRequest.getId() == null;
        Event event;

        if (isNew) {
            event = new Event();
            event.setUnion(user.getUnion()); // 👈 CRITICAL: Stamp with Union!
        } else {
            event = eventRepository.findById(eventRequest.getId())
                    .orElseThrow(() -> new RuntimeException("Event not found"));

            verifyEventBelongsToUnion(event);
        }

        event.setTitle(eventRequest.getTitle());
        event.setDescription(eventRequest.getDescription());
        event.setStart(eventRequest.getStart());
        event.setEnd(eventRequest.getEnd());
        event.setLocation(eventRequest.getLocation());
        event.setEventType(eventRequest.getEventType() != null ? eventRequest.getEventType() : EventType.GENERAL);


        if (eventRequest.getAttendees() != null && !eventRequest.getAttendees().isEmpty()) {
            List<Member> potentialAttendees = memberRepository.findAllById(eventRequest.getAttendees());

            // 🛡️ Security Check: Are all these members in my union?
            // If someone hacked the request to add Member ID 999 (from another union), this stops them.
            for (Member m : potentialAttendees) {
                verifyMemberBelongsToUnion(m);
            }
            event.setAttendees(new HashSet<>(potentialAttendees));
        } else {
            event.setAttendees(new HashSet<>());
        }

        Event savedEvent = eventRepository.save(event);

        // ... Event Publisher code ...
        EventSavedEventDto dto = new EventSavedEventDto(
                savedEvent.getId(),
                savedEvent.getTitle(),
                savedEvent.getDescription(),
                savedEvent.getStart(),
                savedEvent.getEnd(),
                user.getId(), // Use the user passed in
                isNew
        );
        eventPublisher.publishEvent(dto);

        return savedEvent;
    }


    @Override
    public void deleteEvent(Event event) {
        verifyEventBelongsToUnion(event);
        eventRepository.delete(event);
    }



    private Union getCurrentUserUnion() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof MyUser) {
            return ((MyUser) principal).getUnion();
        }
        throw new RuntimeException("No user logged in or user is not of type MyUser");
    }

    private void verifyMemberBelongsToUnion(Member member) {
        Union currentUnion = getCurrentUserUnion();
        if (!member.getUnion().getId().equals(currentUnion.getId())) {
            throw new AccessDeniedException("ACCESS DENIED: You do not have permission to view/edit this member.");
        }
    }

    private void verifyEventBelongsToUnion(Event event) {
        Union currentUnion = getCurrentUserUnion();
        if (!event.getUnion().getId().equals(currentUnion.getId())) {
            throw new AccessDeniedException("ACCESS DENIED: You do not have permission to view/edit this event.");
        }
    }


}
