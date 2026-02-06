package org.example.civitaswebapp.controller;

import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.domain.Notification;
import org.example.civitaswebapp.service.communication.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

        List<Notification> notifications = notificationService.getRecentNotifications(user, 5);
        List<Map<String, Object>> response = notifications.stream().map(n -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", n.getId());
            map.put("title", n.getTitle());
            map.put("message", n.getMessage());
            map.put("status", n.getStatus());
            map.put("url", n.getUrl());
            map.put("createdAt", n.getCreatedAt());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/notifications/unread-count")
    public ResponseEntity<Long> getUnreadCount(@AuthenticationPrincipal MyUser user) {
        long count = notificationService.getUnreadCount(user);
        return ResponseEntity.ok(count);
    }


    @PostMapping("/api/notifications/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, @AuthenticationPrincipal MyUser user) {
        notificationService.markAsRead(id, user);
        return ResponseEntity.ok().build();
    }
}