package org.example.civitaswebapp.controller;


import org.example.civitaswebapp.domain.Event;
import org.example.civitaswebapp.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
public class EventRestController {

    @Autowired
    private EventService eventService;

    @GetMapping
    public List<Map<String, Object>> getEvents() {
        return eventService.getEvents(Pageable.unpaged())
                .getContent()
                .stream()
                .map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", e.getId());
                    map.put("title", e.getTitle());
                    map.put("start", e.getStart().toString());
                    map.put("end", e.getEnd().toString());
                    map.put("description", e.getDescription());
                    return map;
                })
                .toList();
    }



    @PostMapping
    public Event createEvent(@RequestBody Event event) {
        return eventService.saveEvent(event);
    }
}

