package com.example.gestionservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO envoyé au Wallet lors d'une demande de paiement.
 * Le PIN est transmis temporairement et jamais stocké.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletPaymentRequest {

    private UUID accountId;
    private UUID serviceRequestId;
    private BigDecimal amount;
    private String description;
    /** PIN reçu du client, transmis au Wallet, jamais stocké ni loggé. */
    private String pin;
    /** Clé d'idempotence unique par tentative. */
    private UUID idempotencyKey;
}
