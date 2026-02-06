package org.example.civitaswebapp.service.communication;

import jakarta.persistence.EntityNotFoundException;
import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.domain.Notification;
import org.example.civitaswebapp.domain.NotificationStatus;
import org.example.civitaswebapp.domain.NotificationType;
import org.example.civitaswebapp.exceptions.NotificationNotFoundException;
import org.example.civitaswebapp.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Override
    public Notification createNotification(MyUser user, String title, String message, NotificationType type, String url) {
        System.out.println("📢 createNotification called for user: " + user.getId());
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .url(url)
                .status(NotificationStatus.UNREAD)
                .createdAt(Instant.now())
                .build();
        Notification saved = notificationRepository.save(notification);
        System.out.println("📢 Notification saved: " + saved.getId());
        return saved;
    }


    @Override
    public Page<Notification> getAllNotifications(MyUser user, Pageable pageable) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }

    @Override
    public List<Notification> getRecentNotifications(MyUser user, int limit) {
        return notificationRepository
                .findByUserOrderByCreatedAtDesc(user, PageRequest.of(0, limit))
                .getContent();
    }

    @Override
    public long getUnreadCount(MyUser user) {
        return notificationRepository.countByUserAndStatus(user, NotificationStatus.UNREAD);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, MyUser user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new SecurityException("User not authorized to update this notification");
        }

        notification.setStatus(NotificationStatus.READ);
        notificationRepository.save(notification);
    }


    @Override
    public void markAllAsRead(MyUser user) {
        List<Notification> notifications = notificationRepository.findByUserOrderByCreatedAtDesc(user, Pageable.unpaged()).getContent();

        for (Notification n : notifications){
            n.setStatus(NotificationStatus.READ);
        }
        notificationRepository.saveAll(notifications);
    }

    @Override
    @Transactional
    public void deleteNotification(Long notificationId, MyUser user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new SecurityException("User not authorized to delete this notification");
        }

        notificationRepository.delete(notification);
    }

}
