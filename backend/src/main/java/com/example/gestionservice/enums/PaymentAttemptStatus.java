package com.example.gestionservice.enums;

/**
 * Statut d'une tentative de paiement (PaymentAttempt).
 * Utilisé pour le suivi de l'idempotence et la compensation.
 */
public enum PaymentAttemptStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED
}
