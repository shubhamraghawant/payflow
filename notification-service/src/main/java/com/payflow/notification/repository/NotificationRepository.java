package com.payflow.notification.repository;

import com.payflow.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findBySeverityOrderByCreatedAtDesc(Notification.Severity severity);
    List<Notification> findByAcknowledgedFalseOrderByCreatedAtDesc();
}
