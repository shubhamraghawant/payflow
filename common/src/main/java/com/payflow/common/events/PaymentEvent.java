package com.payflow.common.events;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event published when a payment is initiated.
 * Consumed by fraud-detection-service.
 */
public record PaymentEvent(
        String paymentId,
        BigDecimal amount,
        String currency,
        String senderId,
        String receiverId,
        Instant timestamp
) {}
