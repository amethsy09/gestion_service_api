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
 * <p><b>Identité UUID (Étape 5).</b> {@code accountId} est l'identifiant
 * interne de gestion-service : il est à la fois la valeur du claim {@code sub}
 * du JWT local et la clé de lecture de {@code gestion_account.id}.</p>
 *
 * <pre>
 * JWT sub (UUID) → AccountSecurityResolver.resolve(accountId) → ce principal
 * </pre>
 *
 * <p>Les rôles sont gérés localement par gestion-service et ne proviennent
 * <b>pas</b> du JWT : ils sont relus en base à chaque requête, ce qui permet
 * de révoquer un rôle sans attendre l'expiration du jeton.</p>
 *
 * <p>{@code telephone} provient de {@code GestionAccount.telephone} (lu en
 * base), et non plus du claim {@code sub}. Il est conservé inchangé car il
 * est transmis au Wallet lors des paiements, sans conversion UUID ↔ long.</p>
 *
 * <p>La signature du constructeur n'a pas changé : {@code getAccountId()},
 * {@code getRole()} / {@code getRoles()} et {@code getTelephone()} restent
 * disponibles et utilisés par {@code ServiceRequestController} et
 * {@code PaymentController}.</p>
 */
@Getter
@AllArgsConstructor
public class JwtAuthenticationPrincipal implements UserDetails {

    /**
     * UUID interne de l'identité gestion-service
     * ({@code gestion_account.id}, égale au claim {@code sub}).
     */
    private final UUID accountId;

    /** Rôles métier locaux relus en base (jamais extraits du JWT). */
    private final List<Role> roles;

    /**
     * Téléphone du compte, issu de {@code GestionAccount.telephone}.
     * Utilisé pour les appels au Wallet (identification du compte bancaire).
     */
    private final String telephone;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.name()))
                .toList();
    }

    /**
     * Rôle métier unique du compte, lu en base.
     * Raccourci pour le code métier qui n'a besoin que du rôle principal.
     *
     * @return le rôle du compte, ou {@code null} si aucun rôle n'est porté
     */
    public Role getRole() {
        return (roles == null || roles.isEmpty()) ? null : roles.get(0);
    }

    @Override
    public String getPassword() {
        return null;
    }

    /**
     * Retourne l'UUID interne de gestion-service, sous forme de chaîne.
     * Utilisé par le code métier pour l'ownership des demandes
     * ({@code principal.getAccountId()} côté contrôleurs).
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
