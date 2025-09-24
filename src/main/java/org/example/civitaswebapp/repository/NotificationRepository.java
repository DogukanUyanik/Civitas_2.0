package org.example.civitaswebapp.repository;

import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.domain.Notification;
import org.example.civitaswebapp.domain.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Get recent notifications for a user, sorted newest first
    List<Notification> findTop10ByUserOrderByCreatedAtDesc(MyUser user);

    // Get all notifications for a user with pagination
    Page<Notification> findByUserOrderByCreatedAtDesc(MyUser user, Pageable pageable);

    // Count unread notifications
    long countByUserAndStatus(MyUser user, NotificationStatus status);


}
