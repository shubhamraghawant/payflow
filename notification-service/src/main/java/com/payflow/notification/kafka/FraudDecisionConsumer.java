package com.payflow.notification.kafka;

import com.payflow.common.enums.FraudDecision;
import com.payflow.common.events.FraudDecisionEvent;
import com.payflow.notification.entity.Notification;
import com.payflow.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class FraudDecisionConsumer {

    private final NotificationRepository notificationRepository;

    @KafkaListener(topics = "fraud.decision", groupId = "notification-group")
    public void consume(FraudDecisionEvent event) {
        log.info("Received fraud decision for payment {}: {}", event.paymentId(), event.decision());

        Notification.Severity severity = event.decision() == FraudDecision.FLAGGED
                ? Notification.Severity.CRITICAL
                : Notification.Severity.INFO;

        String message = event.decision() == FraudDecision.FLAGGED
                ? String.format("FRAUD ALERT: Payment %s flagged. Reason: %s", event.paymentId(), event.reason())
                : String.format("Payment %s approved successfully.", event.paymentId());

        Notification notification = Notification.builder()
                .paymentId(event.paymentId())
                .message(message)
                .severity(severity)
                .acknowledged(false)
                .createdAt(Instant.now())
                .build();

        notificationRepository.save(notification);

        if (severity == Notification.Severity.CRITICAL) {
            log.warn("🚨 FRAUD ALERT: Payment {} - {}", event.paymentId(), event.reason());
        }
    }
}
