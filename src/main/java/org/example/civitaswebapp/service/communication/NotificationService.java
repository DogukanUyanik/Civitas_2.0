package org.example.civitaswebapp.service.communication;


import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.domain.Notification;
import org.example.civitaswebapp.domain.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationService {

    /**
     * Creates a notification from i18n message-bundle keys plus runtime arguments. The literal
     * text is never stored — it is resolved in the viewer's locale at display time. Pass
     * {@code messageArgs} for the values substituted into {@code messageKey} (e.g. a member name);
     * use {@code null} or an empty list when the message takes no arguments.
     */
    Notification createNotification(MyUser user, String titleKey, String messageKey,
                                    List<String> messageArgs, NotificationType type, String url);

     Page<Notification> getAllNotifications(MyUser user, Pageable pageable);

    List<Notification> getRecentNotifications(MyUser user, int limit);


    long getUnreadCount(MyUser user);

     void markAsRead(Long notificationId, MyUser user);
     void markAllAsRead(MyUser user);

     void deleteNotification(Long notificationId, MyUser user);


}
