package org.example.civitaswebapp.controller;

import org.example.civitaswebapp.domain.Event;
import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.dto.events.EventRequest;
import org.example.civitaswebapp.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventRestController {

    @Autowired
    private EventService eventService;

    @GetMapping
    public List<Event> getEvents() {
        return eventService.getEvents(Pageable.unpaged()).getContent();
    }

    @PostMapping
    public Event createEvent(@RequestBody EventRequest event, @AuthenticationPrincipal MyUser user) {
        Event savedEvent = eventService.saveEvent(event, user);
        return savedEvent;
    }
}