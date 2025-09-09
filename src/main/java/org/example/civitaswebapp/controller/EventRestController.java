package org.example.civitaswebapp.controller;

import org.example.civitaswebapp.domain.Event;
import org.example.civitaswebapp.dto.EventRequest;
import org.example.civitaswebapp.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventRestController {

    @Autowired
    private EventService eventService;

    @GetMapping
    public List<Event> getEvents() {
        // Return events directly - Spring will serialize them to JSON
        return eventService.getEvents(Pageable.unpaged()).getContent();
    }

    @PostMapping
    public Event createEvent(@RequestBody EventRequest event) {
        System.out.println("Received event: " + event); // DEBUG: Print received event
        Event savedEvent = eventService.saveEvent(event);
        System.out.println("Saved event: " + savedEvent); // DEBUG: Print saved event
        return savedEvent;
    }
}