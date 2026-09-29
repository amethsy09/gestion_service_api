package com.example.gestionservice.entity;

import com.example.gestionservice.enums.ResourceAvailabilityStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Ressource humaine pouvant être affectée à des prestations et tâches.
 */
@Entity
@Table(name = "resources", indexes = {
        @Index(name = "idx_resources_email", columnList = "email"),
        @Index(name = "idx_resources_availability", columnList = "availability_status")
})
@Getter
@Setter
@SuperBuilder
public class Resource extends BaseEntity {

    protected Resource() {
    }

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specialty_id", nullable = false)
    private Specialty specialty;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", nullable = false)
    @Builder.Default
    private ResourceAvailabilityStatus availabilityStatus = ResourceAvailabilityStatus.AVAILABLE;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @OneToMany(targetEntity = PrestationResource.class, cascade =CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "resource")
    @Builder.Default
    private List<PrestationResource> prestationResources = new ArrayList<>();

    @OneToMany(targetEntity = Task.class, cascade =CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "resource")
    @Builder.Default
    private List<Task> tasks = new ArrayList<>();
}