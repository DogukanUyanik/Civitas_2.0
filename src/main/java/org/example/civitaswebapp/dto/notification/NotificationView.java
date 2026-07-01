package org.example.civitaswebapp.dto.notification;

import lombok.Builder;
import lombok.Getter;
import org.example.civitaswebapp.domain.NotificationStatus;
import org.example.civitaswebapp.domain.NotificationType;

import java.time.Instant;

/**
 * Display-ready projection of a notification: title/message are already resolved into the viewer's
 * locale. Exposes JavaBean getters so the Thymeleaf template can keep using {@code ${note.title}},
 * {@code ${note.status}}, etc.
 */
@Getter
@Builder
public class NotificationView {
    private final Long id;
    private final String title;
    private final String message;
    private final NotificationType type;
    private final NotificationStatus status;
    private final String url;
    private final Instant createdAt;
}
