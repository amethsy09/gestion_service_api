package com.example.gestionservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Réponse du Wallet après traitement d'un paiement.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletPaymentResponse {

    /** SUCCESS | FAILED */
    private String status;

    /** Référence unique de transaction côté Wallet. */
    private String transactionReference;

    private UUID idempotencyKey;

    /** Message d'erreur en cas d'échec. */
    private String failureReason;
}
