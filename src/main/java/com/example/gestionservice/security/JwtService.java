package com.example.gestionservice.security;

import com.example.gestionservice.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Service de validation des JWT émis par le Wallet externe.
 * Ce microservice ne génère JAMAIS de JWT — il valide uniquement.
 */
@Slf4j
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expiration;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    /**
     * Valide le token JWT et retourne les claims.
     * @throws JwtException si le token est invalide ou expiré.
     */
    public Claims validateAndGetClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Extrait l'accountId (UUID) depuis les claims du JWT.
     * Le JWT du Wallet contient accountId dans le claim "accountId" ou "sub".
     */
    public UUID extractAccountId(Claims claims) {
        // Tentative sur "accountId" d'abord, sinon "sub"
        String accountIdStr = claims.get("accountId", String.class);
        if (accountIdStr == null) {
            accountIdStr = claims.getSubject();
        }
        if (accountIdStr == null) {
            throw new JwtException("accountId introuvable dans le JWT");
        }
        return UUID.fromString(accountIdStr);
    }

    /**
     * Extrait la liste des rôles depuis le claim "roles" du JWT.
     */
    @SuppressWarnings("unchecked")
    public List<Role> extractRoles(Claims claims) {
        List<String> roleStrings = claims.get("roles", List.class);
        if (roleStrings == null || roleStrings.isEmpty()) {
            return List.of(Role.ROLE_USER); // rôle par défaut
        }
        return roleStrings.stream()
                .map(r -> {
                    try {
                        // Accepte "USER", "ROLE_USER", "ADMIN", "ROLE_ADMIN" …
                        String normalized = r.startsWith("ROLE_") ? r : "ROLE_" + r;
                        return Role.valueOf(normalized);
                    } catch (IllegalArgumentException e) {
                        log.warn("Rôle inconnu dans le JWT : {}", r);
                        return Role.ROLE_USER;
                    }
                })
                .toList();
    }

    public boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }
}
