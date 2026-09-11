package com.example.gestionservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO pour initier ou retenter un paiement.
 * Le PIN est reçu temporairement, transmis au Wallet et jamais stocké ni loggé.
 */
@Getter
@Setter
public class PaymentRequest {

    @NotBlank(message = "Le PIN est obligatoire")
    @Size(min = 4, max = 6, message = "Le PIN doit contenir entre 4 et 6 chiffres")
    private String pin;
}
