package org.example.civitaswebapp.service;

import org.example.civitaswebapp.domain.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface EventService {

    Page<Event> getEvents(Pageable pageable);

    //Page<Event> getEvents(Pageable pageable, String search);

    Optional<Event> findById(Long id);

    Event saveEvent(Event event);

    void deleteEvent(Event event);
}
