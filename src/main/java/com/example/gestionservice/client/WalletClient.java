package com.example.gestionservice.client;

import com.example.gestionservice.client.dto.WalletPaymentRequest;
import com.example.gestionservice.client.dto.WalletPaymentResponse;
import com.example.gestionservice.client.dto.WalletPaymentStatusResponse;
import com.example.gestionservice.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

/**
 * Client Feign vers le Wallet Service (banque1_api).
 * Ce microservice est propriétaire de : solde, PIN, JWT, transactions.
 *
 * <p>L'endpoint de paiement externe est {@code /api/transactions/paiement-externe}
 * (interne à banque1_api). Le contrat de requête est {@link WalletPaymentRequest}
 * (téléphone + PIN + montant + idempotencyKey) et la réponse est
 * {@link WalletPaymentResponse}.</p>
 */
@FeignClient(
        name = "wallet-service",
        url = "${wallet-service.url}",
        configuration = FeignConfig.class
)
public interface WalletClient {

    /**
     * Initie un paiement via le Wallet (banque1_api).
     * Le Wallet vérifie le PIN, le solde et effectue le débit.
     * L'idempotencyKey évite les doubles débits (timeout/réessai).
     */
    @PostMapping("/api/transactions/paiement-externe")
    WalletPaymentResponse pay(@RequestBody WalletPaymentRequest request);

    /**
     * Vérifie le statut d'un paiement par sa clé d'idempotence.
     * Utilisé pour la synchronisation en cas de timeout côté appelant.
     */
    @GetMapping("/api/transactions/paiement-externe/status")
    WalletPaymentStatusResponse getPaymentStatus(@RequestParam("idempotencyKey") UUID idempotencyKey);

}
