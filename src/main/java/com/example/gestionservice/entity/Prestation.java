package com.example.gestionservice.entity;

import com.example.gestionservice.enums.PrestationStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Prestation liée à une demande de service.
 * Une demande ne peut avoir qu'une seule prestation.
 * Une prestation ne peut démarrer qu'après paiement.
 */
@Entity
@Table(name = "prestations", indexes = {
        @Index(name = "idx_prestations_status", columnList = "status")
})
@Getter
@Setter
@SuperBuilder
public class Prestation extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_request_id", nullable = false, unique = true)
    private ServiceRequest serviceRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_id", nullable = false)
    private Responsible responsible;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private PrestationStatus status = PrestationStatus.WAITING_PAYMENT;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "estimated_end_date")
    private LocalDate estimatedEndDate;

    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;

    @OneToMany(targetEntity = PrestationResource.class, cascade =CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "prestation")
    @Builder.Default
    private List<PrestationResource> prestationResources = new ArrayList<>();

    @OneToMany(targetEntity = Task.class, cascade =CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "prestation")
    @Builder.Default
    private List<Task> tasks = new ArrayList<>();
}