package com.payflow.payment.service;

import com.payflow.common.enums.PaymentStatus;
import com.payflow.common.events.PaymentEvent;
import com.payflow.payment.dto.CreatePaymentRequest;
import com.payflow.payment.dto.PaymentResponse;
import com.payflow.payment.entity.Payment;
import com.payflow.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Core business logic for payment creation, retrieval, and status management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private static final String PAYMENT_INITIATED_TOPIC = "payment.initiated";

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    /**
     * Creates a new payment, persists it, and publishes a PaymentEvent to Kafka.
     *
     * @param request the payment creation request
     * @return the created payment as a response DTO
     */
    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        Payment payment = Payment.builder()
                .amount(request.amount())
                .currency(request.currency().toUpperCase())
                .senderId(request.senderId())
                .receiverId(request.receiverId())
                .status(PaymentStatus.PENDING)
                .build();

        Payment saved = paymentRepository.save(payment);
        log.info("Payment created with id={}, amount={} {}", saved.getId(), saved.getAmount(), saved.getCurrency());

        PaymentEvent event = new PaymentEvent(
                saved.getId().toString(),
                saved.getAmount(),
                saved.getCurrency(),
                saved.getSenderId(),
                saved.getReceiverId(),
                Instant.now()
        );

        kafkaTemplate.send(PAYMENT_INITIATED_TOPIC, saved.getId().toString(), event);
        log.info("PaymentEvent published to topic={} for paymentId={}", PAYMENT_INITIATED_TOPIC, saved.getId());

        return toResponse(saved);
    }

    /**
     * Retrieves a payment by its ID.
     *
     * @param id the payment UUID
     * @return the payment response
     * @throws jakarta.persistence.EntityNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Payment not found: " + id));
        return toResponse(payment);
    }

    /**
     * Returns a paginated list of all payments.
     *
     * @param pageable pagination parameters
     * @return page of payment responses
     */
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getAllPayments(Pageable pageable) {
        return paymentRepository.findAll(pageable).map(this::toResponse);
    }

    /**
     * Updates the status of a payment. Called internally by the fraud decision consumer.
     *
     * @param id     the payment UUID
     * @param status the new status to set
     */
    @Transactional
    public void updatePaymentStatus(UUID id, PaymentStatus status) {
        paymentRepository.findById(id).ifPresentOrElse(payment -> {
            payment.setStatus(status);
            paymentRepository.save(payment);
            log.info("Payment id={} status updated to {}", id, status);
        }, () -> log.warn("Payment id={} not found for status update to {}", id, status));
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getSenderId(),
                payment.getReceiverId(),
                payment.getStatus(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
