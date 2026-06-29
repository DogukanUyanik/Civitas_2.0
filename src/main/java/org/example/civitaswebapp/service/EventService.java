package org.example.civitaswebapp.service;

import org.example.civitaswebapp.domain.Event;
import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.dto.events.EventRequest;
import org.example.civitaswebapp.dto.events.EventResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface EventService {

    Page<EventResponseDto> getEvents(Pageable pageable);

    Optional<Event> findById(Long id);

    EventResponseDto saveEvent(EventRequest eventRequest, MyUser user);

    void deleteEvent(Event event);
}
