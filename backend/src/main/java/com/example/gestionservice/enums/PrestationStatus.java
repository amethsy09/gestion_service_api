package com.example.gestionservice.enums;

/**
 * Statut du cycle de vie d'une prestation.
 *
 * Workflow :
 * DRAFT → WAITING_PAYMENT → PAID → PLANNED → IN_PROGRESS → COMPLETED
 *                                           ↘ CANCELLED
 *
 * Règle : une prestation ne peut pas démarrer (IN_PROGRESS) avant d'être PAID.
 */
public enum PrestationStatus {
    DRAFT,
    WAITING_PAYMENT,
    PAID,
    PLANNED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
