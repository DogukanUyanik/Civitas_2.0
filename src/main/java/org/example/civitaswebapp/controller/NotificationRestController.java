package org.example.civitaswebapp.controller;

import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.domain.Notification;
import org.example.civitaswebapp.service.MyUserService;
import org.example.civitaswebapp.service.communication.NotificationMessageResolver;
import org.example.civitaswebapp.service.communication.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class NotificationRestController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private MyUserService myUserService;

    @Autowired
    private NotificationMessageResolver notificationMessageResolver;

    @GetMapping("/api/notifications/recent")
    public ResponseEntity<List<Map<String, Object>>> getRecentNotifications() {

        MyUser user = myUserService.getLoggedInUser();
        Locale locale = LocaleContextHolder.getLocale();
        List<Notification> notifications = notificationService.getRecentNotifications(user, 5);
        List<Map<String, Object>> response = notifications.stream().map(n -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", n.getId());
            // Resolve to the viewer's locale so the JS-rendered dropdown matches the page.
            map.put("title", notificationMessageResolver.resolveTitle(n, locale));
            map.put("message", notificationMessageResolver.resolveMessage(n, locale));
            map.put("status", n.getStatus());
            map.put("url", n.getUrl());
            map.put("createdAt", n.getCreatedAt());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/notifications/unread-count")
    public ResponseEntity<Long> getUnreadCount() {
        long count = notificationService.getUnreadCount(myUserService.getLoggedInUser());
        return ResponseEntity.ok(count);
    }


    @PostMapping("/api/notifications/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id, myUserService.getLoggedInUser());
        return ResponseEntity.ok().build();
    }
}