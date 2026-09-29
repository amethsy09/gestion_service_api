package com.example.gestionservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Spécialité d'une ressource humaine.
 * Exemples : Backend Developer, Frontend Developer, DevOps Engineer…
 */
@Entity
@Table(name = "specialties")
@Getter
@Setter
@SuperBuilder
public class Specialty extends BaseEntity {

    protected Specialty() {
    }

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @OneToMany(targetEntity = Resource.class, cascade =CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "specialty")
    @Builder.Default
    private List<Resource> resources = new ArrayList<>();
}