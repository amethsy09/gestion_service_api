package com.example.gestionservice.security;

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

/**
 * Service de validation des JWT émis par auth_api.
 *
 * <p>Le JWT de auth_api contient :
 * <pre>
 * {
 *   "sub": "telephone",
 *   "iat": "...",
 *   "exp": "..."
 * }
 * </pre>
 *
 * Ce microservice ne génère JAMAIS de JWT — il valide uniquement
 * la signature, vérifie l'expiration et extrait le {@code sub} (téléphone).
 *
 * Le téléphone est ensuite résolu en identité locale via
 * {@link AccountSecurityResolver} → {@code gestion_account}.
 */
@Slf4j
@Service
public class JwtService {

    private final SecretKey signingKey;

    public JwtService(@Value("${jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Valide le token JWT (signature + expiration) et retourne les claims.
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
     * Extrait le téléphone depuis le claim {@code sub} du JWT.
     * Le JWT de auth_api ne contient PAS d'accountId ni de roles —
     * seul le {@code sub} (téléphone) est utilisé.
     *
     * @throws JwtException si {@code sub} est absent ou vide
     */
    public String extractTelephone(Claims claims) {
        String telephone = claims.getSubject();
        if (telephone == null || telephone.isBlank()) {
            throw new JwtException("Claim 'sub' (téléphone) absent ou vide dans le JWT");
        }
        return telephone;
    }

    /**
     * Vérifie si le JWT est expiré.
     */
    public boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }
}
