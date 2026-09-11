package com.example.gestionservice.entity;

import com.example.gestionservice.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * Identité locale de gestion-service.
 *
 * <p>Le JWT émis par auth_api contient uniquement {@code sub = telephone}.
 * Cette table fait le lien entre le téléphone et :</p>
 * <ul>
 *   <li>l'UUID interne de gestion-service ({@code id})</li>
 *   <li>les rôles métier (ADMIN / RESPONSIBLE / USER)</li>
 *   <li>l'identifiant du compte bancaire chez le Wallet ({@code walletAccountId}, de type long)</li>
 * </ul>
 *
 * <p>Cette table ne remplace PAS le Wallet : le Wallet reste propriétaire
 * des comptes bancaires, soldes, PIN et transactions.</p>
 */
@Entity
@Table(name = "gestion_account", indexes = {
        @Index(name = "idx_gestion_account_telephone", columnList = "telephone"),
        @Index(name = "idx_gestion_account_wallet_id", columnList = "wallet_account_id"),
        @Index(name = "idx_gestion_account_active", columnList = "active")
})
@Getter
@Setter
@SuperBuilder
public class GestionAccount extends BaseEntity {

    /**
     * Téléphone tel quel extrait du JWT (claim {@code sub}).
     * UNIQUE — c'est la clé de résolution d'identité.
     */
    @Column(name = "telephone", nullable = false, unique = true, length = 20)
    private String telephone;

    /**
     * Identifiant du compte bancaire chez le Wallet (type {@code long}).
     * Peut être null si pas encore résolu.
     * JAMAIS converti en UUID — types différents.
     */
    @Column(name = "wallet_account_id")
    private Long walletAccountId;

    /**
     * Rôle métier géré localement par gestion-service.
     * JAMAIS extrait du JWT.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    @Builder.Default
    private Role role = Role.ROLE_USER;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;
}
