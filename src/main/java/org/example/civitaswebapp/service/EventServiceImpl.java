package org.example.civitaswebapp.service;

import org.example.civitaswebapp.domain.*;
import org.example.civitaswebapp.dto.events.EventRequest;
import org.example.civitaswebapp.dto.events.EventResponseDto;
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

    @Autowired
    private MyUserService myUserService;

    @Override
    public Page<EventResponseDto> getEvents(Pageable pageable) {
        return eventRepository.findAllByUnion(getCurrentUserUnion(), pageable).map(this::toResponseDto);
    }

    @Override
    public Optional<Event> findById(Long id) {
        return eventRepository.findByIdAndUnion(id, getCurrentUserUnion());
    }



    @Transactional
    @Override
    public EventResponseDto saveEvent(EventRequest eventRequest, MyUser user) {
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


        List<Member> potentialAttendees = List.of();
        if (eventRequest.getAttendees() != null && !eventRequest.getAttendees().isEmpty()) {
            potentialAttendees = memberRepository.findAllById(eventRequest.getAttendees());

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

        // Capture the attendee phone numbers HERE, on the request thread, while the members are
        // already loaded (phoneNumber is a scalar column — no collection load). The async listener
        // then never reloads the event or its attendees PersistentSet, so the two threads can never
        // race on the same Hibernate collection.
        List<String> attendeePhoneNumbers = potentialAttendees.stream()
                .map(Member::getPhoneNumber)
                .filter(phone -> phone != null && !phone.isBlank())
                .toList();

        EventSavedEventDto dto = new EventSavedEventDto(
                savedEvent.getId(),
                savedEvent.getTitle(),
                savedEvent.getDescription(),
                savedEvent.getStart(),
                savedEvent.getEnd(),
                savedEvent.getLocation(),
                savedEvent.getEventType() != null ? savedEvent.getEventType().name() : EventType.GENERAL.name(),
                user.getId(), // Use the user passed in
                isNew,
                attendeePhoneNumbers
        );
        eventPublisher.publishEvent(dto);

        return toResponseDto(savedEvent);
    }

    private EventResponseDto toResponseDto(Event event) {
        return new EventResponseDto(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getStart(),
                event.getEnd(),
                event.getLocation(),
                event.getEventType() != null ? event.getEventType().name() : "GENERAL"
        );
    }


    @Override
    public void deleteEvent(Event event) {
        verifyEventBelongsToUnion(event);
        eventRepository.delete(event);
    }



    private Union getCurrentUserUnion() {
        // Reload a fresh, request-scoped Union rather than reading it off the shared session
        // principal — the principal no longer carries a managed entity graph (see MyUserPrincipal).
        return myUserService.getLoggedInUser().getUnion();
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
