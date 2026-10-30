package com.payflow.notification.controller;

import com.payflow.notification.entity.Notification;
import com.payflow.notification.repository.NotificationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification management API")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping
    @Operation(summary = "Get all notifications paginated")
    public ResponseEntity<Page<Notification>> getAllNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(notificationRepository.findAll(
                PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/alerts")
    @Operation(summary = "Get critical fraud alerts")
    public ResponseEntity<List<Notification>> getAlerts() {
        return ResponseEntity.ok(notificationRepository.findBySeverityOrderByCreatedAtDesc(Notification.Severity.CRITICAL));
    }

    @PatchMapping("/{id}/acknowledge")
    @Operation(summary = "Acknowledge a notification")
    public ResponseEntity<Notification> acknowledge(@PathVariable UUID id) {
        return notificationRepository.findById(id)
                .map(n -> {
                    n.setAcknowledged(true);
                    return ResponseEntity.ok(notificationRepository.save(n));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
