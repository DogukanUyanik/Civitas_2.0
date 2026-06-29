package org.example.civitaswebapp.controller;

import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.dto.notification.NotificationView;
import org.example.civitaswebapp.service.MyUserService;
import org.example.civitaswebapp.service.communication.NotificationMessageResolver;
import org.example.civitaswebapp.service.communication.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private MyUserService myUserService;

    @Autowired
    private NotificationMessageResolver notificationMessageResolver;

    @GetMapping
    public String showNotificationsList(
            Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        MyUser currentLoggedInUser = myUserService.getLoggedInUser();
        var notificationPage = notificationService.getAllNotifications(currentLoggedInUser, PageRequest.of(page, size));

        Locale locale = LocaleContextHolder.getLocale();
        List<NotificationView> notifications = notificationPage.getContent().stream()
                .map(n -> NotificationView.builder()
                        .id(n.getId())
                        .title(notificationMessageResolver.resolveTitle(n, locale))
                        .message(notificationMessageResolver.resolveMessage(n, locale))
                        .type(n.getType())
                        .status(n.getStatus())
                        .url(n.getUrl())
                        .createdAt(n.getCreatedAt())
                        .build())
                .toList();

        model.addAttribute("notifications", notifications);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", notificationPage.getTotalPages());
        model.addAttribute("totalItems", notificationPage.getTotalElements());

        model.addAttribute("currentUri", "/notifications");

        return "notifications/notificationsList";
    }

    @PostMapping("/mark-all-read")
    public String markAllAsRead() {
        notificationService.markAllAsRead(myUserService.getLoggedInUser());
        return "redirect:/notifications";
    }
}