package org.example.civitaswebapp.service;

import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.domain.Notification;
import org.example.civitaswebapp.domain.NotificationType;
import org.example.civitaswebapp.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Override
    public Notification createNotification(MyUser user, String title, String message, NotificationType type, String url) {

        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .type(type)
                .url(url)
                .build();
        return notificationRepository.save(notification);

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
        return 0;
    }

    @Override
    public void markAsRead(Long notificationId, MyUser user) {

    }

    @Override
    public void markAllAsRead(MyUser user) {

    }

    @Override
    public void deleteNotification(Long notificationId, MyUser user) {

    }
}
