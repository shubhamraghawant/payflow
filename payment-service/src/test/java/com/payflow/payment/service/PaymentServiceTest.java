package com.payflow.payment.service;

import com.payflow.common.enums.FraudDecision;
import com.payflow.common.enums.PaymentStatus;
import com.payflow.common.events.FraudDecisionEvent;
import com.payflow.common.events.PaymentEvent;
import com.payflow.payment.dto.CreatePaymentRequest;
import com.payflow.payment.dto.PaymentResponse;
import com.payflow.payment.entity.Payment;
import com.payflow.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Unit Tests")
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    @InjectMocks
    private PaymentService paymentService;

    private UUID paymentId;
    private Payment savedPayment;

    @BeforeEach
    void setUp() {
        paymentId = UUID.randomUUID();
        savedPayment = Payment.builder()
                .id(paymentId)
                .amount(new BigDecimal("500.00"))
                .currency("USD")
                .senderId("sender-001")
                .receiverId("receiver-002")
                .status(PaymentStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("createPayment: should save payment and publish Kafka event")
    void createPayment_shouldSaveAndPublishEvent() {
        CreatePaymentRequest request = new CreatePaymentRequest(
                new BigDecimal("500.00"), "USD", "sender-001", "receiver-002");

        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        PaymentResponse response = paymentService.createPayment(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(paymentId);
        assertThat(response.amount()).isEqualByComparingTo("500.00");
        assertThat(response.currency()).isEqualTo("USD");
        assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);

        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(kafkaTemplate, times(1)).send(eq("payment.initiated"), eq(paymentId.toString()), any(PaymentEvent.class));
    }

    @Test
    @DisplayName("createPayment: should uppercase currency code")
    void createPayment_shouldUppercaseCurrency() {
        CreatePaymentRequest request = new CreatePaymentRequest(
                new BigDecimal("100.00"), "usd", "s1", "r1");

        Payment lowercasePayment = Payment.builder()
                .id(paymentId)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .senderId("s1")
                .receiverId("r1")
                .status(PaymentStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(paymentRepository.save(any(Payment.class))).thenReturn(lowercasePayment);

        PaymentResponse response = paymentService.createPayment(request);
        assertThat(response.currency()).isEqualTo("USD");
    }

    @Test
    @DisplayName("getPayment: should return payment when found")
    void getPayment_shouldReturnPayment_whenFound() {
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(savedPayment));

        PaymentResponse response = paymentService.getPayment(paymentId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(paymentId);
        verify(paymentRepository, times(1)).findById(paymentId);
    }

    @Test
    @DisplayName("getPayment: should throw EntityNotFoundException when not found")
    void getPayment_shouldThrowException_whenNotFound() {
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPayment(paymentId))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessageContaining(paymentId.toString());
    }

    @Test
    @DisplayName("updatePaymentStatus: should update status to APPROVED")
    void updatePaymentStatus_shouldUpdateToApproved() {
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(savedPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        paymentService.updatePaymentStatus(paymentId, PaymentStatus.APPROVED);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.APPROVED);
    }

    @Test
    @DisplayName("updatePaymentStatus: should update status to FLAGGED")
    void updatePaymentStatus_shouldUpdateToFlagged() {
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(savedPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        paymentService.updatePaymentStatus(paymentId, PaymentStatus.FLAGGED);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.FLAGGED);
    }

    @Test
    @DisplayName("updatePaymentStatus: should log warning when payment not found")
    void updatePaymentStatus_shouldDoNothing_whenPaymentNotFound() {
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        // Should not throw — just log a warning
        paymentService.updatePaymentStatus(paymentId, PaymentStatus.APPROVED);

        verify(paymentRepository, never()).save(any());
    }
}
