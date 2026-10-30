package com.payflow.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.common.enums.PaymentStatus;
import com.payflow.payment.dto.CreatePaymentRequest;
import com.payflow.payment.dto.PaymentResponse;
import com.payflow.payment.service.PaymentService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@DisplayName("PaymentController Integration Tests")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    private final UUID testPaymentId = UUID.randomUUID();

    private PaymentResponse buildPaymentResponse(PaymentStatus status) {
        return new PaymentResponse(
                testPaymentId,
                new BigDecimal("500.00"),
                "USD",
                "sender-001",
                "receiver-002",
                status,
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("POST /api/v1/payments - should create payment and return 201")
    void createPayment_shouldReturn201() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest(
                new BigDecimal("500.00"), "USD", "sender-001", "receiver-002");

        when(paymentService.createPayment(any())).thenReturn(buildPaymentResponse(PaymentStatus.PENDING));

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(testPaymentId.toString()))
                .andExpect(jsonPath("$.amount").value(500.00))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/v1/payments - should return 400 when amount is null")
    void createPayment_shouldReturn400_whenAmountIsNull() throws Exception {
        String invalidJson = """
                {
                  "currency": "USD",
                  "senderId": "sender-001",
                  "receiverId": "receiver-002"
                }
                """;

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/payments - should return 400 when senderId is blank")
    void createPayment_shouldReturn400_whenSenderIdIsBlank() throws Exception {
        String invalidJson = """
                {
                  "amount": 100.00,
                  "currency": "USD",
                  "senderId": "",
                  "receiverId": "receiver-002"
                }
                """;

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/payments/{id} - should return payment when found")
    void getPayment_shouldReturn200_whenFound() throws Exception {
        when(paymentService.getPayment(testPaymentId)).thenReturn(buildPaymentResponse(PaymentStatus.APPROVED));

        mockMvc.perform(get("/api/v1/payments/{id}", testPaymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testPaymentId.toString()))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("GET /api/v1/payments/{id} - should return 404 when not found")
    void getPayment_shouldReturn404_whenNotFound() throws Exception {
        when(paymentService.getPayment(testPaymentId))
                .thenThrow(new EntityNotFoundException("Payment not found: " + testPaymentId));

        mockMvc.perform(get("/api/v1/payments/{id}", testPaymentId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/payments - should return paginated payments")
    void getAllPayments_shouldReturn200_withPaginatedResults() throws Exception {
        List<PaymentResponse> payments = List.of(
                buildPaymentResponse(PaymentStatus.APPROVED),
                buildPaymentResponse(PaymentStatus.PENDING)
        );
        PageImpl<PaymentResponse> page = new PageImpl<>(payments, PageRequest.of(0, 10), 2);

        when(paymentService.getAllPayments(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/payments")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }
}
