package com.example.gestionservice.support;

import com.example.gestionservice.entity.GestionAccount;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Factory unique de jetons JWT pour les tests.
 *
 * <p>Centralise la construction des JWT afin que les tests ne les signent plus à la main.
 * La signature utilise le <b>même</b> secret que l'application
 * ({@code jwt.secret} / {@code JWT_SECRET}), sans quoi
 * {@code JwtService.validateLocalToken} rejetterait le jeton.</p>
 *
 * <p>Depuis l'Étape 5, un seul format est accepté par le pipeline :</p>
 *
 * <table border="1">
 *   <caption>Formats JWT gérés</caption>
 *   <tr><th>Format</th><th>Claim {@code sub}</th><th>Usage</th></tr>
 *   <tr>
 *     <td>Local (seul accepté)</td>
 *     <td>{@code gestion_account.id} (UUID)</td>
 *     <td>{@link #localTokenForAccount(GestionAccount)} — format émis par
 *         {@code JwtService.generateToken} et accepté par
 *         {@code AccountSecurityResolver}.</td>
 *   </tr>
 *   <tr>
 *     <td>Ancien (rejeté)</td>
 *     <td>numéro de téléphone</td>
 *     <td>{@link #legacyTokenForTelephone(String)} — conservé
 *         <b>uniquement</b> pour vérifier que ce format est désormais
 *         refusé (ni {@code iss}, ni UUID).</td>
 *   </tr>
 * </table>
 *
 * <p>Ne jamais logger un jeton produit par cette classe.</p>
 */
public final class JwtTestTokenFactory {

    /**
     * Émetteur qui figurera dans le claim {@code iss} du format cible.
     * Doit rester aligné sur {@code jwt.issuer} de l'application.
     * Vérifié par {@code JwtService.validateLocalToken}.
     */
    public static final String DEFAULT_ISSUER = "gestion-service";

    /**
     * Durée de validité par défaut des jetons de test.
     * Alignée sur la durée habituelle d'un jeton d'accès court.
     */
    public static final Duration DEFAULT_VALIDITY = Duration.ofHours(1);

    private final SecretKey signingKey;
    private final String issuer;
    private final Duration validity;

    /**
     * Factory avec l'émetteur et la durée par défaut.
     *
     * @param secret valeur de {@code jwt.secret} (32 caractères minimum pour HS256)
     */
    public JwtTestTokenFactory(String secret) {
        this(secret, DEFAULT_ISSUER, DEFAULT_VALIDITY);
    }

    public JwtTestTokenFactory(String secret, String issuer, Duration validity) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.validity = validity;
    }

    // ================================================================
    //  Ancien format — sub = téléphone (rejeté par le pipeline)
    // ================================================================

    /**
     * Jeton au format historique {@code sub = telephone}, désormais refusé.
     *
     * <p>Utilisé uniquement par les tests qui vérifient le rejet de ce
     * format à l'Étape 5. Il ne doit plus servir à authentifier une requête.</p>
     */
    public String legacyTokenForTelephone(String telephone) {
        return legacyTokenForTelephone(telephone, validity);
    }

    public String legacyTokenForTelephone(String telephone, Duration tokenValidity) {
        return baseBuilder(tokenValidity)
                .setSubject(telephone)
                .compact();
    }

    // ================================================================
    //  Format local — sub = gestion_account.id (seul format accepté)
    // ================================================================

    /**
     * Jeton au format de l'authentification locale : {@code sub} porte l'UUID
     * interne du compte, et non plus le téléphone.
     */
    public String localTokenForAccount(GestionAccount account) {
        return localTokenForAccountId(account.getId(), validity);
    }

    public String localTokenForAccountId(UUID accountId) {
        return localTokenForAccountId(accountId, validity);
    }

    public String localTokenForAccountId(UUID accountId, Duration tokenValidity) {
        return baseBuilder(tokenValidity)
                .setSubject(accountId.toString())
                .setIssuer(issuer)
                .compact();
    }

    /**
     * Jeton local porteur d'un émetteur arbitraire — pour vérifier que
     * {@code JwtService.validateLocalToken} rejette un {@code iss} inconnu.
     */
    public String localTokenForAccountId(UUID accountId, String tokenIssuer) {
        return baseBuilder(validity)
                .setSubject(accountId.toString())
                .setIssuer(tokenIssuer)
                .compact();
    }

    /**
     * Jeton local (émetteur valide) dont le {@code sub} n'est pas forcément un
     * UUID — pour vérifier que {@code JwtService.extractAccountId} rejette
     * explicitement un subject non conforme, dont un numéro de téléphone.
     */
    public String localTokenWithSubject(String subject) {
        return baseBuilder(validity)
                .setSubject(subject)
                .setIssuer(issuer)
                .compact();
    }

    // ================================================================
    //  Utilitaires
    // ================================================================

    /**
     * Jeton construit à partir d'un claim {@code sub} arbitraire.
     * Réservé aux tests de robustesse de {@code JwtService}
     * (subject vide, subject non UUID, etc.).
     */
    public String tokenWithSubject(String subject, Duration tokenValidity) {
        return baseBuilder(tokenValidity)
                .setSubject(subject)
                .compact();
    }

    /**
     * Jeton <b>sans</b> claim {@code exp}.
     *
     * <p>Le JWT ne rend pas {@code exp} obligatoire : JJWT accepte un tel jeton
     * et renvoie {@code getExpiration() == null}. Sans vérification explicite,
     * toute lecture de cette valeur provoquerait une {@code NullPointerException}.</p>
     */
    public String tokenWithoutExpiration(String subject) {
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(Date.from(Instant.now()))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Jeton signé avec une autre clé — pour vérifier le rejet d'une
     * signature invalide.
     */
    public String tokenSignedWithOtherSecret(String subject, String otherSecret, Duration tokenValidity) {
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(Date.from(Instant.now()))
                .setExpiration(Date.from(Instant.now().plus(tokenValidity)))
                .signWith(Keys.hmacShaKeyFor(otherSecret.getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Jeton déjà expiré — pour vérifier le rejet d'un jeton hors délai.
     */
    public String expiredTokenForTelephone(String telephone) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(telephone)
                .setIssuedAt(Date.from(now.minus(Duration.ofHours(2))))
                .setExpiration(Date.from(now.minus(Duration.ofHours(1))))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    private io.jsonwebtoken.JwtBuilder baseBuilder(Duration tokenValidity) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(tokenValidity)))
                .signWith(signingKey, SignatureAlgorithm.HS256);
    }

    /**
     * Téléphone unique et plausible, pour éviter les collisions entre tests
     * sur la contraintes UNIQUE de {@code gestion_account.telephone}.
     * Format sénégalais : 9 chiffres commençant par 77.
     */
    public static String uniqueTelephone() {
        String digits = UUID.randomUUID().toString().replaceAll("[^0-9]", "");
        while (digits.length() < 7) {
            digits += UUID.randomUUID().toString().replaceAll("[^0-9]", "");
        }
        return "77" + digits.substring(0, 7);
    }
}
