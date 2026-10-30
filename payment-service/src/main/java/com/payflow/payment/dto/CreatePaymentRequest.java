package com.payflow.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request DTO for creating a new payment.
 */
@Schema(description = "Request payload for initiating a payment")
public record CreatePaymentRequest(

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        @Schema(description = "Payment amount", example = "250.00")
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
        @Schema(description = "ISO 4217 currency code", example = "USD")
        String currency,

        @NotBlank(message = "Sender ID is required")
        @Schema(description = "Unique identifier of the payment sender", example = "user-001")
        String senderId,

        @NotBlank(message = "Receiver ID is required")
        @Schema(description = "Unique identifier of the payment receiver", example = "user-002")
        String receiverId
) {}
