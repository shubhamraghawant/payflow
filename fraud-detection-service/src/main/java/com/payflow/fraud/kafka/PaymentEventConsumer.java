package com.payflow.fraud.kafka;

import com.payflow.common.events.FraudDecisionEvent;
import com.payflow.common.events.PaymentEvent;
import com.payflow.fraud.service.FraudRulesEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for payment.initiated events.
 * Evaluates each payment through the fraud rules engine and publishes the verdict.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private static final String FRAUD_DECISION_TOPIC = "fraud.decision";

    private final FraudRulesEngine fraudRulesEngine;
    private final KafkaTemplate<String, FraudDecisionEvent> kafkaTemplate;

    @KafkaListener(
            topics = "payment.initiated",
            groupId = "fraud-detection-group",
            containerFactory = "paymentEventKafkaListenerContainerFactory"
    )
    public void consume(PaymentEvent event) {
        log.info("Received PaymentEvent: paymentId={}, amount={} {}",
                event.paymentId(), event.amount(), event.currency());

        FraudDecisionEvent decision = fraudRulesEngine.evaluate(event);

        kafkaTemplate.send(FRAUD_DECISION_TOPIC, event.paymentId(), decision);
        log.info("FraudDecisionEvent published: paymentId={}, decision={}, reason={}",
                decision.paymentId(), decision.decision(), decision.reason());
    }
}
