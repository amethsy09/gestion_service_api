package com.example.gestionservice.client;

import com.example.gestionservice.client.dto.WalletPaymentRequest;
import com.example.gestionservice.client.dto.WalletPaymentResponse;
import com.example.gestionservice.client.dto.WalletPaymentStatusResponse;
import com.example.gestionservice.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

/**
 * Client Feign vers le Wallet Service externe.
 * Ce microservice est propriétaire de : solde, PIN, JWT, transactions.
 */
@FeignClient(
        name = "wallet-service",
        url = "${wallet-service.url}",
        configuration = FeignConfig.class
)
public interface WalletClient {

    /**
     * Initie un paiement via le Wallet.
     * Le Wallet vérifie JWT, PIN, solde et effectue le débit.
     */
    @PostMapping("/api/v1/internal/payments/service")
    WalletPaymentResponse pay(@RequestBody WalletPaymentRequest request);

    /**
     * Vérifie le statut d'un paiement par idempotencyKey.
     * Utilisé avant un retry pour éviter les doubles débits (cas timeout).
     */
    @GetMapping("/api/v1/internal/payments/status/{idempotencyKey}")
    WalletPaymentStatusResponse getPaymentStatus(@PathVariable("idempotencyKey") UUID idempotencyKey);
}
