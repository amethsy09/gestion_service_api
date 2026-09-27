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
 *   <li>les rôles métier (ADMIN / RESPONSABLE / USER)</li>
 *   <li>l'identifiant du compte bancaire chez le Wallet ({@code walletAccountId}, de type long)</li>
 * </ul>
 *
 * <p>Cette table ne remplace PAS le Wallet : le Wallet reste propriétaire
 * des comptes bancaires, soldes, PIN et transactions.</p>
 *
 * <p>À l'Étape 3 (authentification locale), les champs {@code fullName},
 * {@code email} et {@code password} sont ajoutés pour permettre
 * l'inscription et la connexion autonomes.</p>
 */
@Entity
@Table(name = "gestion_account", indexes = {
        @Index(name = "idx_gestion_account_telephone", columnList = "telephone"),
        @Index(name = "idx_gestion_account_email", columnList = "email"),
        @Index(name = "idx_gestion_account_wallet_id", columnList = "wallet_account_id"),
        @Index(name = "idx_gestion_account_active", columnList = "active")
})
@Getter
@Setter
@SuperBuilder
public class GestionAccount extends BaseEntity {

    protected GestionAccount() {
    }

    /**
     * Nom complet de l'utilisateur.
     * Obligatoire, non vide (uniquement des espaces rejeté).
     */
    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    /**
     * Adresse email de l'utilisateur.
     * UNIQUE — clé secondaire d'authentification.
     * Normalisé en minuscules avant stockage.
     */
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    /**
     * Téléphone tel quel extrait du JWT (claim {@code sub}).
     * UNIQUE — c'est l'identifiant de connexion principal.
     */
    @Column(name = "telephone", nullable = false, unique = true, length = 20)
    private String telephone;

    /**
     * Mot de passe hashé BCrypt.
     * JAMAIS exposer en réponse API.
     * Stocké sous forme de hash (60 caractères pour BCrypt).
     */
    @Column(name = "password", length = 100)
    private String password;

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
     * Lors d'une inscription publique, toujours {@code ROLE_USER}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    @Builder.Default
    private Role role = Role.ROLE_USER;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    /**
     * Indique si l'utilisateur doit changer son mot de passe à la prochaine connexion.
     * <p>
     * À <code>true</code> pour les comptes créés avec un mot de passe temporaire
     * (par exemple par un admin lors de la création d'un responsable).
     * La connexion est alors refusée jusqu'à ce que l'utilisateur ait défini
     * un nouveau mot de passe.
     * </p>
     */
    @Column(name = "must_change_password", nullable = false)
    @Builder.Default
    private Boolean mustChangePassword = false;
}