package com.example.gestionservice.entity;

import com.example.gestionservice.enums.ResourceAssignmentStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Table d'association entre Prestation et Resource (relation N:N).
 * Gère le cycle de vie de l'affectation d'une ressource à une prestation.
 */
@Entity
@Table(
    name = "prestation_resources",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_prestation_resource",
            columnNames = {"prestation_id", "resource_id"}
        )
    },
    indexes = {
        @Index(name = "idx_prestation_resources_prestation", columnList = "prestation_id"),
        @Index(name = "idx_prestation_resources_resource", columnList = "resource_id")
    }
)
@Getter
@Setter
@SuperBuilder
public class PrestationResource extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prestation_id", nullable = false)
    private Prestation prestation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @Column(name = "unassigned_at")
    private LocalDateTime unassignedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ResourceAssignmentStatus status = ResourceAssignmentStatus.ASSIGNED;
}