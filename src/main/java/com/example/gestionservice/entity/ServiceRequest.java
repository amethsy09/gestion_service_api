package com.example.gestionservice.entity;

import com.example.gestionservice.enums.PaymentStatus;
import com.example.gestionservice.enums.ServiceRequestStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Demande de service émise par un client (identifié par accountId depuis le JWT).
 * Aucune relation JPA vers une entité User — le Wallet reste propriétaire des utilisateurs.
 */
@Entity
@Table(name = "service_requests", indexes = {
        @Index(name = "idx_service_requests_account_id", columnList = "account_id"),
        @Index(name = "idx_service_requests_status", columnList = "status")
})
@Getter
@Setter
@SuperBuilder
public class ServiceRequest extends BaseEntity {

    /**
     * Identifiant du compte client provenant du Wallet.
     * Récupéré uniquement depuis le JWT — jamais fourni par le client dans le body.
     */
    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_catalog_id", nullable = false)
    private ServiceCatalog serviceCatalog;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ServiceRequestStatus status = ServiceRequestStatus.WAITING_PAYMENT;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @OneToOne(targetEntity = Prestation.class, cascade =CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "serviceRequest")
    private Prestation prestation;

    @OneToMany(targetEntity = PaymentAttempt.class, cascade =CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "serviceRequest")
    @Builder.Default
    private List<PaymentAttempt> paymentAttempts = new ArrayList<>();
}