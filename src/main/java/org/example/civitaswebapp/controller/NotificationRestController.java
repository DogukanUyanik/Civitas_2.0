package org.example.civitaswebapp.controller;


import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.domain.Notification;
import org.example.civitaswebapp.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class NotificationRestController {

    @Autowired
    private NotificationService notificationService;


    @GetMapping("/api/notifications/recent")
    public ResponseEntity<List<Notification>> getRecentNotifications(@AuthenticationPrincipal MyUser user) {
        List<Notification> notifications = notificationService.getRecentNotifications(user, 10);
        return ResponseEntity.ok(notifications);
    }

    // Fetch unread count
    @GetMapping("/api/notifications/unread-count")
    public ResponseEntity<Long> getUnreadCount(@AuthenticationPrincipal MyUser user) {
        long count = notificationService.getUnreadCount(user);
        return ResponseEntity.ok(count);
    }
}
