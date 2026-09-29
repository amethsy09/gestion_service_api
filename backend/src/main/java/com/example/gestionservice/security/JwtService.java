package com.example.gestionservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Service de validation <b>et d'émission</b> des JWT.
 *
 * <p>Depuis l'Étape 5 (cutover définitif {@code telephone → UUID}), un seul
 * format de jeton est accepté :</p>
 *
 * <pre>
 * {
 *   "sub": "&lt;gestion_account.id — UUID&gt;",
 *   "iss": "gestion-service",
 *   "iat": "...",
 *   "exp": "..."
 * }
 * </pre>
 *
 * <p>Il est émis par {@link #generateToken(UUID)} et correspond à l'identité
 * locale de {@code gestion_account}. L'ancien format {@code sub = telephone}
 * n'est <b>plus accepté par le pipeline de sécurité</b> : il ne porte ni
 * émetteur, ni UUID, et {@link #validateLocalToken(String)} le rejette.</p>
 *
 * <p><b>Séparation des étapes de validation.</b> {@link #validateAndGetClaims(String)}
 * est l'étape cryptographique : signature, expiration et présence d'un
 * {@code exp}. {@link #validateLocalToken(String)} y ajoute la contrainte
 * d'émetteur propre au format local, et {@link #extractAccountId(Claims)}
 * la contrainte de format du {@code sub}. C'est l'ensemble de ces trois
 * contrôles que {@code JwtAuthenticationFilter} applique à chaque requête.</p>
 *
 * <p>Le rôle n'est <b>pas</b> placé dans le JWT : il est relu en base à chaque
 * requête, ce qui permet de révoquer un rôle sans attendre l'expiration du
 * token. Le jeton ne contient ni téléphone, ni email, ni nom, ni mot de passe.
 * Aucun mot de passe ni donnée sensible n'est inclus.</p>
 */
@Slf4j
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final String issuer;
    private final long accessTokenExpirationMs;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.issuer:gestion-service}") String issuer,
                      @Value("${jwt.expiration:3600000}") long accessTokenExpirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.accessTokenExpirationMs = accessTokenExpirationMs;
    }

    // ================================================================
    //  ÉMISSION — format local
    // ================================================================

    /**
     * Émet un JWT local pour un compte de {@code gestion_account}.
     *
     * <p>Claims produits :</p>
     * <pre>
     * {
     *   "sub": "&lt;uuid&gt;",   // gestion_account.id
     *   "iss": "gestion-service",
     *   "iat": "...",
     *   "exp": "..."
     * }
     * </pre>
     *
     * <p>Aucun rôle, aucun téléphone, aucun mot de passe : l'identité et les
     * habilitations sont relues en base, le téléphone ne sert qu'à identifier
     * le compte auprès du Wallet lors d'un paiement.</p>
     *
     * @param accountId UUID interne de {@code gestion_account}
     * @return jeton signé, prêt à être placé dans un header {@code Authorization: Bearer}
     * @throws IllegalArgumentException si {@code accountId} est nul
     */
    public String generateToken(UUID accountId) {
        if (accountId == null) {
            throw new IllegalArgumentException("accountId ne peut pas être nul");
        }
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(accountId.toString())
                .setIssuer(issuer)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(Duration.ofMillis(accessTokenExpirationMs))))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // ================================================================
    //  VALIDATION
    // ================================================================

    /**
     * Étape cryptographique de la validation : signature, expiration et
     * présence d'un {@code exp} exploitable.
     *
     * <p>La vérification du {@code sub} n'est <b>pas</b> faite ici : c'est
     * {@link #validateLocalToken(String)} qui impose l'émetteur, puis
     * {@link #extractAccountId(Claims)} qui impose le format UUID du sujet.
     * Cette séparation permet à {@code JwtAuthenticationFilter} de rendre
     * chaque rejet explicite.</p>
     *
     * @throws io.jsonwebtoken.security.SignatureException si la signature est invalide
     * @throws io.jsonwebtoken.ExpiredJwtException          si le token est expiré
     * @throws JwtException                                 si le token est illisible
     *                                                   ou si {@code exp} est absent
     */
    public Claims validateAndGetClaims(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        // JJWT n'exige pas la présence de 'exp' : un token sans 'exp' est
        // accepté et renvoie exp = null. Toute lecture ultérieure de
        // getExpiration() produirait alors une NullPointerException.
        // On rejette donc explicitement, avec une erreur explicite.
        if (claims.getExpiration() == null) {
            throw new JwtException("Claim 'exp' absent du JWT : expiration indéterminée");
        }
        return claims;
    }

    /**
     * Valide un jeton et exige en plus le format local : émetteur connu.
     *
     * <p>C'est le validateur appelé par {@code JwtAuthenticationFilter}. Il
     * rejette les anciens jetons {@code sub = telephone}, qui ne portent pas
     * d'émetteur : la compatibilité avec l'ancien format est
     * <b>définitivement supprimée</b>.</p>
     *
     * @throws JwtException si l'émetteur est absent ou différent de {@code jwt.issuer}
     */
    public Claims validateLocalToken(String token) {
        Claims claims = validateAndGetClaims(token);

        String tokenIssuer = claims.getIssuer();
        if (tokenIssuer == null || !issuer.equals(tokenIssuer)) {
            throw new JwtException("Émetteur JWT invalide : attendu '" + issuer
                    + "', obtenu '" + tokenIssuer + "'");
        }
        return claims;
    }

    // ================================================================
    //  EXTRACTION
    // ================================================================

    /**
     * Extrait l'UUID du compte depuis le claim {@code sub} d'un jeton local.
     *
     * <p>Rejette explicitement un {@code sub} qui n'est pas un UUID : un
     * ancien jeton {@code sub = telephone} est donc refusé ici, et non
     * traité comme un identifiant de compte.</p>
     *
     * @throws JwtException si {@code sub} est absent, vide ou n'est pas un UUID
     */
    public UUID extractAccountId(Claims claims) {
        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new JwtException("Claim 'sub' (identifiant de compte) absent ou vide dans le JWT");
        }
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException e) {
            throw new JwtException(
                    "Claim 'sub' n'est pas un UUID valide : '" + subject + "'", e);
        }
    }

    // ================================================================
    //  Utilitaires
    // ================================================================

    /** Durée de vie des jetons locaux, en millisecondes (propriété {@code jwt.expiration}). */
    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }

    /** Émetteur attendu des jetons locaux (propriété {@code jwt.issuer}). */
    public String getIssuer() {
        return issuer;
    }
}
