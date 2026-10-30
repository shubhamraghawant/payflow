package com.payflow.fraud.service;

import com.payflow.common.enums.FraudDecision;
import com.payflow.common.events.FraudDecisionEvent;
import com.payflow.common.events.PaymentEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FraudRulesEngineTest {

    private FraudRulesEngine engine;

    @BeforeEach
    void setUp() {
        engine = new FraudRulesEngine();
    }

    private PaymentEvent buildEvent(String senderId, BigDecimal amount, String currency) {
        return new PaymentEvent(UUID.randomUUID().toString(), amount, currency, senderId, "receiver-1", Instant.now());
    }

    @Test
    void shouldApproveValidPayment() {
        PaymentEvent event = buildEvent("sender-1", new BigDecimal("500.00"), "USD");
        FraudDecisionEvent result = engine.evaluate(event);
        assertThat(result.decision()).isEqualTo(FraudDecision.APPROVED);
    }

    @Test
    void shouldFlagHighAmountPayment() {
        PaymentEvent event = buildEvent("sender-2", new BigDecimal("15000.00"), "USD");
        FraudDecisionEvent result = engine.evaluate(event);
        assertThat(result.decision()).isEqualTo(FraudDecision.FLAGGED);
        assertThat(result.reason()).containsIgnoringCase("threshold");
    }

    @Test
    void shouldFlagUnsupportedCurrency() {
        PaymentEvent event = buildEvent("sender-3", new BigDecimal("100.00"), "XYZ");
        FraudDecisionEvent result = engine.evaluate(event);
        assertThat(result.decision()).isEqualTo(FraudDecision.FLAGGED);
        assertThat(result.reason()).containsIgnoringCase("currency");
    }

    @Test
    void shouldFlagRateLimitExceeded() {
        String senderId = "spammer-sender";
        for (int i = 0; i < 3; i++) {
            engine.evaluate(buildEvent(senderId, new BigDecimal("50.00"), "USD"));
        }
        FraudDecisionEvent result = engine.evaluate(buildEvent(senderId, new BigDecimal("50.00"), "USD"));
        assertThat(result.decision()).isEqualTo(FraudDecision.FLAGGED);
        assertThat(result.reason()).containsIgnoringCase("rate");
    }
}
