package com.example.gestionservice.dto.response;

import com.example.gestionservice.enums.PaymentAttemptStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentAttemptResponse {

    private UUID id;
    private UUID serviceRequestId;
    private UUID accountId;
    private BigDecimal amount;
    private PaymentAttemptStatus status;
    private String walletTransactionReference;
    private UUID idempotencyKey;
    private String failureReason;
    private Integer attemptNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
