package com.example.gestionservice.enums;

/**
 * Statut du cycle de vie d'une demande de service.
 *
 * Workflow :
 * CREATED → WAITING_PAYMENT → PAID → IN_PROGRESS → COMPLETED
 *                                  ↘ CANCELLED
 */
public enum ServiceRequestStatus {
    CREATED,
    WAITING_PAYMENT,
    PAID,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
