package org.example.civitaswebapp.controller;


import org.example.civitaswebapp.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/events")
public class EventPageController {


    @Autowired
    private EventService eventService;

    @GetMapping
    public String listEvents() {
        return "events";
    }
}
