package org.example.civitaswebapp.controller;

import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.dto.events.EventRequest;
import org.example.civitaswebapp.dto.events.EventResponseDto;
import org.example.civitaswebapp.service.EventService;
import org.example.civitaswebapp.service.MyUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventRestController {

    @Autowired
    private EventService eventService;

    @Autowired
    private MyUserService myUserService;

    @GetMapping
    public List<EventResponseDto> getEvents() {
        return eventService.getEvents(Pageable.unpaged()).getContent();
    }

    @PostMapping
    public EventResponseDto createEvent(@RequestBody EventRequest event) {
        // Reload a fresh, request-scoped MyUser instead of using the shared session principal.
        MyUser user = myUserService.getLoggedInUser();
        return eventService.saveEvent(event, user);
    }
}