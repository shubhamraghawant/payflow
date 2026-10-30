package com.payflow.payment.kafka;

import com.payflow.common.enums.FraudDecision;
import com.payflow.common.enums.PaymentStatus;
import com.payflow.common.events.FraudDecisionEvent;
import com.payflow.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Kafka consumer for fraud decision events.
 * Updates payment status based on the fraud engine's verdict.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FraudDecisionConsumer {

    private final PaymentService paymentService;

    @KafkaListener(
            topics = "fraud.decision",
            groupId = "payment-service-group",
            containerFactory = "fraudDecisionKafkaListenerContainerFactory"
    )
    public void consume(FraudDecisionEvent event) {
        log.info("Received FraudDecisionEvent: paymentId={}, decision={}, reason={}",
                event.paymentId(), event.decision(), event.reason());

        PaymentStatus newStatus = event.decision() == FraudDecision.APPROVED
                ? PaymentStatus.APPROVED
                : PaymentStatus.FLAGGED;

        paymentService.updatePaymentStatus(UUID.fromString(event.paymentId()), newStatus);
    }
}
