package com.payflow.fraud.service;

import com.payflow.common.enums.FraudDecision;
import com.payflow.common.events.FraudDecisionEvent;
import com.payflow.common.events.PaymentEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fraud rules evaluation engine.
 *
 * <p>Rules applied in order:
 * <ol>
 *   <li>RULE 1: Amount exceeds $10,000 threshold → FLAGGED</li>
 *   <li>RULE 2: Unsupported currency → FLAGGED</li>
 *   <li>RULE 3: Same sender submits more than 3 payments within 60 seconds → FLAGGED (rate limit)</li>
 * </ol>
 *
 * <p>If all rules pass → APPROVED
 */
@Service
@Slf4j
public class FraudRulesEngine {

    private static final BigDecimal AMOUNT_THRESHOLD = new BigDecimal("10000");
    private static final long RATE_LIMIT_WINDOW_SECONDS = 60L;
    private static final int RATE_LIMIT_MAX_PAYMENTS = 3;

    private static final Set<String> SUPPORTED_CURRENCIES = Set.of(
            "USD", "EUR", "INR", "GBP", "AUD", "CAD"
    );

    /**
     * ConcurrentHashMap tracking payment timestamps per senderId for rate-limiting.
     * Deques store Instant timestamps of recent payments.
     */
    private final ConcurrentHashMap<String, Deque<Instant>> senderPaymentTimestamps = new ConcurrentHashMap<>();

    /**
     * Evaluates a payment event against all fraud rules.
     *
     * @param event the payment event to evaluate
     * @return a FraudDecisionEvent with the verdict and reason
     */
    public FraudDecisionEvent evaluate(PaymentEvent event) {
        log.info("Evaluating payment: id={}, amount={} {}, sender={}",
                event.paymentId(), event.amount(), event.currency(), event.senderId());

        // RULE 1: Amount threshold check
        if (event.amount().compareTo(AMOUNT_THRESHOLD) > 0) {
            log.warn("RULE 1 triggered: paymentId={}, amount={}", event.paymentId(), event.amount());
            return buildDecision(event.paymentId(), FraudDecision.FLAGGED, "Amount exceeds threshold");
        }

        // RULE 2: Currency support check
        if (!SUPPORTED_CURRENCIES.contains(event.currency().toUpperCase())) {
            log.warn("RULE 2 triggered: paymentId={}, currency={}", event.paymentId(), event.currency());
            return buildDecision(event.paymentId(), FraudDecision.FLAGGED, "Unsupported currency");
        }

        // RULE 3: Rate limit check per sender
        if (isRateLimitExceeded(event.senderId())) {
            log.warn("RULE 3 triggered: paymentId={}, sender={}", event.paymentId(), event.senderId());
            return buildDecision(event.paymentId(), FraudDecision.FLAGGED, "Rate limit exceeded");
        }

        log.info("Payment APPROVED: paymentId={}", event.paymentId());
        return buildDecision(event.paymentId(), FraudDecision.APPROVED, "All checks passed");
    }

    /**
     * Checks if a sender has exceeded the rate limit (more than 3 payments in 60 seconds).
     * Evicts expired timestamps from the deque before evaluating.
     *
     * @param senderId the sender's unique identifier
     * @return true if the rate limit is exceeded
     */
    private boolean isRateLimitExceeded(String senderId) {
        Instant now = Instant.now();
        Instant windowStart = now.minusSeconds(RATE_LIMIT_WINDOW_SECONDS);

        Deque<Instant> timestamps = senderPaymentTimestamps.computeIfAbsent(senderId, k -> new ArrayDeque<>());

        synchronized (timestamps) {
            // Evict old timestamps outside the window
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(windowStart)) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= RATE_LIMIT_MAX_PAYMENTS) {
                return true;
            }

            // Record this payment's timestamp
            timestamps.addLast(now);
            return false;
        }
    }

    private FraudDecisionEvent buildDecision(String paymentId, FraudDecision decision, String reason) {
        return new FraudDecisionEvent(paymentId, decision, reason, Instant.now());
    }
}
