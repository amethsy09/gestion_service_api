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
 * Principal Spring Security construit depuis le JWT émis par le Wallet.
 * Contient accountId (UUID) et les rôles — pas de mot de passe local.
 */
@Getter
@AllArgsConstructor
public class JwtAuthenticationPrincipal implements UserDetails {

    private final UUID accountId;
    private final List<Role> roles;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.name()))
                .toList();
    }

    /** Non utilisé — pas d'authentification locale. */
    @Override public String getPassword() { return null; }

    /** Identifiant unique = accountId string. */
    @Override public String getUsername() { return accountId.toString(); }

    @Override public boolean isAccountNonExpired()  { return true; }
    @Override public boolean isAccountNonLocked()   { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()            { return true; }
}
