package com.payflow.common.events;

import com.payflow.common.enums.FraudDecision;

import java.time.Instant;

/**
 * Event published by fraud-detection-service after evaluating a payment.
 * Consumed by payment-service and notification-service.
 */
public record FraudDecisionEvent(
        String paymentId,
        FraudDecision decision,
        String reason,
        Instant timestamp
) {}
