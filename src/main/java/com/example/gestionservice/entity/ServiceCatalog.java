package com.example.gestionservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Catalogue des services proposés.
 * Un service possède un prix de base utilisé lors de la création d'une demande.
 */
@Entity
@Table(name = "service_catalog")
@Getter
@Setter
@SuperBuilder
public class ServiceCatalog extends BaseEntity {

    protected ServiceCatalog() {
    }

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "base_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @OneToMany(targetEntity = ServiceRequest.class, cascade =CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "serviceCatalog")
    @Builder.Default
    private List<ServiceRequest> serviceRequests = new ArrayList<>();
}