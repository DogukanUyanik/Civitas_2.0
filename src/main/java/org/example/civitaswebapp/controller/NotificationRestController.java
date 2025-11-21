package org.example.civitaswebapp.controller;


import org.example.civitaswebapp.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationRestController {

    @Autowired
    private NotificationService notificationService;


    @GetMapping("/api/notifications/recent")
}
