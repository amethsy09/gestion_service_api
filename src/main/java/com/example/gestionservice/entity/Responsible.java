package com.example.gestionservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsable d'une ou plusieurs prestations.
 * Personne interne à l'entreprise qui pilote la prestation.
 */
@Entity
@Table(name = "responsibles", indexes = {
        @Index(name = "idx_responsibles_email", columnList = "email")
})
@Getter
@Setter
@SuperBuilder
public class Responsible extends BaseEntity {

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @OneToMany(targetEntity = Prestation.class, cascade =CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "responsible")
    @Builder.Default
    private List<Prestation> prestations = new ArrayList<>();
}