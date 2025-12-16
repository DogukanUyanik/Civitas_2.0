package org.example.civitaswebapp.controller;

import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.domain.Notification;
import org.example.civitaswebapp.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class NotificationRestController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/api/notifications/recent")
    public ResponseEntity<List<Map<String, Object>>> getRecentNotifications(@AuthenticationPrincipal MyUser user) {

        // 1. Debug: Let's prove who the system thinks you are
        System.out.println("🔍 DEBUG: Fetching notifications for User ID: " + user.getId() + " - " + user.getUsername());

        List<Notification> notifications = notificationService.getRecentNotifications(user, 5);

        System.out.println("🔍 DEBUG: Found " + notifications.size() + " notifications in DB");

        // 2. Map to a clean structure to avoid "User -> Notification -> User" infinite loops
        List<Map<String, Object>> response = notifications.stream().map(n -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", n.getId());
            map.put("title", n.getTitle());
            map.put("message", n.getMessage());
            map.put("status", n.getStatus());
            map.put("url", n.getUrl());
            map.put("createdAt", n.getCreatedAt()); // Jackson handles LocalDateTime automatically
            // NOTICE: We do NOT put 'user' here. That stops the loop.
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/notifications/unread-count")
    public ResponseEntity<Long> getUnreadCount(@AuthenticationPrincipal MyUser user) {
        long count = notificationService.getUnreadCount(user);
        System.out.println("🔍 DEBUG: Unread count is: " + count);
        return ResponseEntity.ok(count);
    }
}