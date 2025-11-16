package org.example.civitaswebapp.service;

import org.example.civitaswebapp.domain.Event;
import org.example.civitaswebapp.dto.events.EventRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface EventService {

    Page<Event> getEvents(Pageable pageable);

    Optional<Event> findById(Long id);

    Event saveEvent(EventRequest eventRequest);

    void deleteEvent(Event event);
}
