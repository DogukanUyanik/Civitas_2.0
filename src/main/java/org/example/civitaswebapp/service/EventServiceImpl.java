package org.example.civitaswebapp.service;

import org.example.civitaswebapp.domain.Event;
import org.example.civitaswebapp.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class EventServiceImpl implements EventService {

    @Autowired
    private EventRepository eventRepository;

    @Override
    public Page<Event> getEvents(Pageable pageable) {
        return eventRepository.findAll(pageable);
    }

    @Override
    public Optional<Event> findById(Long id) {
        return eventRepository.findById(id);
    }

    @Override
    public Event saveEvent(Event event) {
        eventRepository.save(event);
        return event;
    }

    @Override
    public void deleteEvent(Event event) {
        eventRepository.delete(event);
    }
}
