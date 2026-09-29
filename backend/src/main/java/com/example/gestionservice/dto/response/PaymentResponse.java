package com.example.gestionservice.dto.response;

import com.example.gestionservice.enums.PaymentAttemptStatus;
import com.example.gestionservice.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Réponse enrichie après une opération de paiement.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private UUID serviceRequestId;
    private PaymentStatus serviceRequestPaymentStatus;
    private UUID paymentAttemptId;
    private PaymentAttemptStatus attemptStatus;
    private UUID idempotencyKey;
    private String walletTransactionReference;
    private BigDecimal amount;
    private Integer attemptNumber;
    private String failureReason;
    private LocalDateTime processedAt;
}
