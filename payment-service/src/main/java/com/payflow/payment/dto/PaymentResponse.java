package com.payflow.payment.dto;

import com.payflow.common.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for payment data.
 */
@Schema(description = "Payment details response")
public record PaymentResponse(
        @Schema(description = "Unique payment identifier") UUID id,
        @Schema(description = "Payment amount") BigDecimal amount,
        @Schema(description = "Currency code") String currency,
        @Schema(description = "Sender user ID") String senderId,
        @Schema(description = "Receiver user ID") String receiverId,
        @Schema(description = "Current payment status") PaymentStatus status,
        @Schema(description = "Creation timestamp") Instant createdAt,
        @Schema(description = "Last update timestamp") Instant updatedAt
) {}
