package com.example.gestionservice.entity;

import com.example.gestionservice.enums.PaymentAttemptStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Historique de chaque tentative de paiement d'une demande.
 * Permet l'idempotence (idempotencyKey), la compensation et le retry.
 *
 * Le PIN n'est jamais stocké ici.
 */
@Entity
@Table(name = "payment_attempts", indexes = {
        @Index(name = "idx_payment_attempts_service_request", columnList = "service_request_id"),
        @Index(name = "idx_payment_attempts_account_id", columnList = "account_id"),
        @Index(name = "idx_payment_attempts_idempotency_key", columnList = "idempotency_key", unique = true),
        @Index(name = "idx_payment_attempts_status", columnList = "status")
})
@Getter
@Setter
@SuperBuilder
public class PaymentAttempt extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_request_id", nullable = false)
    private ServiceRequest serviceRequest;

    /**
     * accountId du client — copié depuis la demande pour faciliter les requêtes d'audit.
     */
    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private PaymentAttemptStatus status = PaymentAttemptStatus.PENDING;

    /**
     * Référence de transaction retournée par le Wallet en cas de succès.
     */
    @Column(name = "wallet_transaction_reference")
    private String walletTransactionReference;

    /**
     * Clé d'idempotence unique par tentative — empêche les doubles débits.
     */
    @Column(name = "idempotency_key", nullable = false, unique = true)
    private UUID idempotencyKey;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    /**
     * Numéro de la tentative (1ère, 2ème, 3ème…).
     */
    @Column(name = "attempt_number", nullable = false)
    @Builder.Default
    private Integer attemptNumber = 1;
}