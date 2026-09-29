package com.example.gestionservice.controller;

import com.example.gestionservice.dto.request.PaymentRequest;
import com.example.gestionservice.dto.response.ApiResponse;
import com.example.gestionservice.dto.response.PaymentResponse;
import com.example.gestionservice.security.JwtAuthenticationPrincipal;
import com.example.gestionservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/service-requests")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Paiement des demandes de service via le Wallet externe")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Payer une demande via le Wallet",
               description = "Le PIN est transmis au Wallet et n'est jamais stocké ni loggé")
    public ResponseEntity<ApiResponse<PaymentResponse>> pay(
            @PathVariable UUID id,
            @Valid @RequestBody PaymentRequest request,
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal) {
        PaymentResponse response = paymentService.pay(id, request, principal.getAccountId(), principal.getTelephone());
        return ResponseEntity.ok(ApiResponse.success("Paiement traité", response));
    }

    @PostMapping("/{id}/retry-payment")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Réessayer un paiement échoué",
               description = "Crée une nouvelle tentative avec un nouvel idempotencyKey")
    public ResponseEntity<ApiResponse<PaymentResponse>> retryPayment(
            @PathVariable UUID id,
            @Valid @RequestBody PaymentRequest request,
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal) {
        PaymentResponse response = paymentService.retryPayment(id, request, principal.getAccountId(), principal.getTelephone());
        return ResponseEntity.ok(ApiResponse.success("Nouvelle tentative de paiement traitée", response));
    }

    @PostMapping("/{id}/payment/sync")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Synchroniser le statut du paiement avec le Wallet",
               description = "Vérifie via idempotencyKey si un paiement timeout a été traité par le Wallet")
    public ResponseEntity<ApiResponse<PaymentResponse>> syncPayment(
            @PathVariable UUID id,
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal) {
        PaymentResponse response = paymentService.syncPaymentStatus(id, principal.getAccountId());
        return ResponseEntity.ok(ApiResponse.success("Synchronisation effectuée", response));
    }
}
