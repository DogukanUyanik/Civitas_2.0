package org.example.civitaswebapp.service.communication;


import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.domain.Notification;
import org.example.civitaswebapp.domain.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationService {

    Notification createNotification(MyUser user, String title, String message, NotificationType type, String url);

     Page<Notification> getAllNotifications(MyUser user, Pageable pageable);

    List<Notification> getRecentNotifications(MyUser user, int limit);


    long getUnreadCount(MyUser user);

     void markAsRead(Long notificationId, MyUser user);
     void markAllAsRead(MyUser user);

     void deleteNotification(Long notificationId, MyUser user);


}
