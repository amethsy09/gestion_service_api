package com.example.gestionservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO envoyé au Wallet (banque1_api) lors d'une demande de paiement.
 *
 * <p>Le Wallet identifie le compte par {@code telephone} — pas par un
 * UUID ou long interne à gestion-service. Cela évite toute conversion
 * risquée entre les deux systèmes d'identifiants.</p>
 *
 * <p>Le PIN est transmis temporairement et jamais stocké ni loggé.</p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletPaymentRequest {

    /** Téléphone du titulaire — le Wallet résout le compte bancaire. */
    private String telephone;

    private UUID serviceRequestId;

    private BigDecimal amount;

    private String description;

    /** PIN reçu du client, transmis au Wallet, jamais stocké ni loggé. */
    private String pin;

    /** Clé d'idempotence unique par tentative. */
    private UUID idempotencyKey;
}
