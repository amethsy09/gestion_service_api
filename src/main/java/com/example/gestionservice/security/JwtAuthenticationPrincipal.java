package com.example.gestionservice.security;

import com.example.gestionservice.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Principal Spring Security construit après résolution d'identité.
 *
 * <p><b>Important :</b> {@code accountId} est l'identifiant interne de l'identité
 * de gestion-service, résolu à partir du téléphone contenu dans le JWT.
 * Il ne provient PAS directement du JWT (qui ne contient que {@code sub = telephone}).
 * Il provient de la table {@code gestion_account}.</p>
 *
 * <p>Les rôles sont gérés localement par gestion-service et ne proviennent pas du JWT.</p>
 *
 * <p>{@code telephone} est conservé pour être envoyé au Wallet lors des paiements,
 * sans conversion UUID ↔ long.</p>
 */
@Getter
@AllArgsConstructor
public class JwtAuthenticationPrincipal implements UserDetails {

    /**
     * UUID interne de l'identité gestion-service.
     * Provenant de {@code gestion_account.id}, pas du JWT.
     */
    private final UUID accountId;

    /** Rôles métier locaux (jamais extraits du JWT). */
    private final List<Role> roles;

    /**
     * Téléphone extrait du JWT ({@code sub}).
     * Utilisé pour les appels au Wallet (identification du compte bancaire).
     */
    private final String telephone;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.name()))
                .toList();
    }

    @Override
    public String getPassword() {
        return null;
    }

    /**
     * Retourne l'UUID interne de gestion-service.
     * Utilisé par le code métier pour l'ownership des demandes.
     */
    @Override
    public String getUsername() {
        return accountId.toString();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
