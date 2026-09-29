package com.example.gestionservice.security;

import com.example.gestionservice.support.JwtTestTokenFactory;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests du {@code JwtService} de production — <b>Étape 5</b>.
 *
 * <p>La construction des jetons de test est centralisée dans
 * {@link JwtTestTokenFactory}, qui signe avec le même secret que
 * l'application.</p>
 *
 * <p>Le format accepté est l'<b>unique</b> format local
 * ({@code sub = gestion_account.id} + {@code iss}), émis par
 * {@code generateToken} et validé par {@code validateLocalToken}.</p>
 *
 * <p>L'ancien format {@code sub = telephone} est ici construit uniquement
 * pour vérifier qu'il est désormais <b>rejeté</b> : la compatibilité
 * {@code telephone} a été supprimée à l'Étape 5.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService — Tests unitaires")
class JwtServiceTest {

    /** Doit rester aligné sur {@code jwt.secret} de src/test/resources/application.yml. */
    private static final String SECRET = "test_secret_key_for_junit_tests_32_chars_min";
    private static final String OTHER_SECRET = "another_wrong_secret_key_32_chars_min";
    private static final String ISSUER = "gestion-service";

    /** Doit rester aligné sur {@code jwt.expiration} de src/test/resources/application.yml. */
    private static final long EXPIRATION_MS = 3_600_000L;

    private JwtService jwtService;
    private JwtTestTokenFactory tokenFactory;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, ISSUER, EXPIRATION_MS);
        tokenFactory = new JwtTestTokenFactory(SECRET);
    }

    // ================================================================
    //  Génération — format local
    // ================================================================

    @Test
    @DisplayName("generateToken — produit un token accepté par la validation")
    void generateToken_producesValidToken() {
        UUID accountId = UUID.randomUUID();

        String token = jwtService.generateToken(accountId);

        assertThat(token).isNotBlank();
        assertThatCode(() -> jwtService.validateAndGetClaims(token)).doesNotThrowAnyException();
        assertThatCode(() -> jwtService.validateLocalToken(token)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("generateToken — sub contient l'UUID du compte")
    void generateToken_subIsAccountUuid() {
        UUID accountId = UUID.randomUUID();

        Claims claims = jwtService.validateLocalToken(jwtService.generateToken(accountId));

        assertThat(claims.getSubject()).isEqualTo(accountId.toString());
        assertThat(jwtService.extractAccountId(claims)).isEqualTo(accountId);
    }

    @Test
    @DisplayName("generateToken — iat, exp et iss sont présents")
    void generateToken_containsIatExpIss() {
        UUID accountId = UUID.randomUUID();

        Claims claims = jwtService.validateLocalToken(jwtService.generateToken(accountId));

        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
        assertThat(claims.getIssuer()).isEqualTo(ISSUER);
    }

    @Test
    @DisplayName("generateToken — exp - iat correspond à la durée configurée")
    void generateToken_validityMatchesConfiguredExpiration() {
        Claims claims = jwtService.validateLocalToken(jwtService.generateToken(UUID.randomUUID()));

        long validityMs = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();

        assertThat(validityMs).isEqualTo(EXPIRATION_MS);
        assertThat(jwtService.getAccessTokenExpirationMs()).isEqualTo(EXPIRATION_MS);
    }

    @Test
    @DisplayName("generateToken — la durée est pilotée par la configuration, pas codée en dur")
    void generateToken_honoursCustomConfiguredExpiration() {
        JwtService court = new JwtService(SECRET, ISSUER, 900_000L); // 15 min

        Claims claims = court.validateLocalToken(court.generateToken(UUID.randomUUID()));

        assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime())
                .isEqualTo(900_000L);
    }

    @Test
    @DisplayName("generateToken — n'embarque ni rôle, ni téléphone, ni mot de passe")
    void generateToken_containsNoSensitiveData() {
        String token = jwtService.generateToken(UUID.randomUUID());

        Claims claims = jwtService.validateLocalToken(token);

        assertThat(claims.get("role")).isNull();
        assertThat(claims.get("telephone")).isNull();
        assertThat(claims.get("password")).isNull();
        assertThat(claims.get("pin")).isNull();
        // Le sub est l'UUID, jamais un téléphone.
        assertThat(claims.getSubject()).doesNotContain("@").doesNotContain(" ");
    }

    @Test
    @DisplayName("generateToken — accountId nul est rejeté")
    void generateToken_nullAccountId_throws() {
        assertThatThrownBy(() -> jwtService.generateToken(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("generateToken — l'émetteur et la durée sont exposés par le service")
    void generateToken_issuerAndDurationAreExposed() {
        assertThat(jwtService.getIssuer()).isEqualTo(ISSUER);
        assertThat(jwtService.getAccessTokenExpirationMs()).isEqualTo(EXPIRATION_MS);
    }

    // ================================================================
    //  Validation — format local
    // ================================================================

    @Test
    @DisplayName("Token local — un UUID valide dans sub est accepté")
    void validateLocalToken_validUuidSubject_accepted() {
        UUID accountId = UUID.randomUUID();

        Claims claims = jwtService.validateLocalToken(jwtService.generateToken(accountId));

        assertThat(jwtService.extractAccountId(claims)).isEqualTo(accountId);
    }

    @Test
    @DisplayName("Token local — sub non UUID → rejet explicite")
    void extractAccountId_nonUuidSubject_rejectedExplicitly() {
        // Émetteur valide pour isoler la validation du seul format de 'sub'.
        String token = tokenFactory.localTokenWithSubject("771234567");

        Claims claims = jwtService.validateLocalToken(token);

        assertThatThrownBy(() -> jwtService.extractAccountId(claims))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("n'est pas un UUID valide");
    }

    @Test
    @DisplayName("Token local — sub absent → rejet explicite")
    void extractAccountId_absentSubject_rejected() {
        String token = tokenFactory.localTokenWithSubject(null);

        Claims claims = jwtService.validateLocalToken(token);

        assertThatThrownBy(() -> jwtService.extractAccountId(claims))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("sub");
    }

    @Test
    @DisplayName("Token local — issuer incorrect → rejet")
    void validateLocalToken_wrongIssuer_rejected() {
        String token = tokenFactory.localTokenForAccountId(UUID.randomUUID(), ISSUER + "- usurpé");

        assertThatThrownBy(() -> jwtService.validateLocalToken(token))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("Émetteur JWT invalide");
    }

    @Test
    @DisplayName("Token local — issuer absent → rejeté par validateLocalToken")
    void validateLocalToken_absentIssuer_rejected() {
        // Ancien jeton (sub = téléphone) : il ne porte pas d'iss, il est donc
        // refusé par le validateur local. C'est le rejet du format téléphone.
        String token = tokenFactory.legacyTokenForTelephone("771234567");

        assertThatThrownBy(() -> jwtService.validateLocalToken(token))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("Émetteur JWT invalide");
    }

    @Test
    @DisplayName("Token local — expiré → rejet")
    void validateLocalToken_expired_rejected() {
        String token = tokenFactory.localTokenForAccountId(UUID.randomUUID(), Duration.ofSeconds(-60));

        assertThatThrownBy(() -> jwtService.validateLocalToken(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("Token local — signature falsifiée → rejet")
    void validateLocalToken_forgedSignature_rejected() {
        String token = new JwtTestTokenFactory(OTHER_SECRET)
                .localTokenForAccountId(UUID.randomUUID());

        assertThatThrownBy(() -> jwtService.validateLocalToken(token))
                .isInstanceOf(JwtException.class);
    }

    // ================================================================
    //  Validation — claim exp
    // ================================================================

    @Test
    @DisplayName("Token sans exp → rejet propre, jamais NullPointerException")
    void validateAndGetClaims_absentExpiration_rejectedWithoutNpe() {
        String tokenWithoutExp = tokenFactory.tokenWithoutExpiration("771234567");

        assertThatThrownBy(() -> jwtService.validateAndGetClaims(tokenWithoutExp))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("exp")
                .isNotInstanceOf(NullPointerException.class);
    }

    // ================================================================
    //  Validation — signature et expiration
    // ================================================================

    @Test
    @DisplayName("Token expiré → validateAndGetClaims lève une JwtException")
    void validateAndGetClaims_expiredToken_throwsJwtException() {
        String token = tokenFactory.expiredTokenForTelephone(JwtTestTokenFactory.uniqueTelephone());

        assertThatThrownBy(() -> jwtService.validateAndGetClaims(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("Token expiré — validateLocalToken lève une JwtException")
    void validateLocalToken_expiredToken_throwsJwtException() {
        String token = tokenFactory.localTokenForAccountId(UUID.randomUUID(), Duration.ofSeconds(-60));

        assertThatThrownBy(() -> jwtService.validateLocalToken(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("Token signature invalide → validateAndGetClaims lève une JwtException")
    void validateAndGetClaims_invalidSignature_throwsJwtException() {
        String token = tokenFactory.tokenSignedWithOtherSecret(
                UUID.randomUUID().toString(), OTHER_SECRET, Duration.ofHours(1));

        assertThatThrownBy(() -> jwtService.validateAndGetClaims(token))
                .isInstanceOf(JwtException.class);
    }

    // ================================================================
    //  Cutover — suppression du format sub = telephone
    // ================================================================

    /**
     * Étape 5 : un jeton {@code sub = telephone} ne doit plus jamais être
     * accepté par le pipeline. Il est rejeté dès l'étape émetteur, car il
     * ne porte pas d'{@code iss}.
     */
    @Test
    @DisplayName("Cutover — jeton sub=telephone → rejeté par validateLocalToken")
    void validateLocalToken_legacyTelephoneToken_rejected() {
        String telephone = JwtTestTokenFactory.uniqueTelephone();

        String token = tokenFactory.legacyTokenForTelephone(telephone);

        assertThatThrownBy(() -> jwtService.validateLocalToken(token))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("Émetteur JWT invalide");
    }

    /**
     * Même si un jeton {@code sub = telephone} parvenait à passer l'étape
     * émetteur, l'extraction de l'identifiant refuse le format téléphone.
     */
    @Test
    @DisplayName("Cutover — sub=telephone rejeté même avec un émetteur valide")
    void extractAccountId_telephoneSubject_evenWithValidIssuer_rejected() {
        String telephone = JwtTestTokenFactory.uniqueTelephone();

        // Émetteur valide : seul le format du 'sub' est en cause.
        String token = tokenFactory.localTokenWithSubject(telephone);

        Claims claims = jwtService.validateLocalToken(token);

        assertThatThrownBy(() -> jwtService.extractAccountId(claims))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("n'est pas un UUID valide");
    }

    @Test
    @DisplayName("Cutover — la méthode d'extraction du téléphone n'existe plus")
    void legacyTelephoneExtraction_removedFromService() {
        // Le cutover a supprimé extractTelephone(Claims) : plus aucun moyen de
        // lire un téléphone depuis le JWT au sein du service.
        assertThat(Arrays.stream(JwtService.class.getDeclaredMethods())
                .map(java.lang.reflect.Method::getName))
                .doesNotContain("extractTelephone", "isTokenExpired");
    }

    // ================================================================
    //  Cohérence factory / production
    // ================================================================

    /**
     * La factory et le {@code JwtService} doivent partager la même clé de
     * signature, sinon les tests d'intégration simuleraient à tort un token
     * invalide.
     */
    @Test
    @DisplayName("La factory et JwtService partagent la même clé de signature")
    void factory_andProductionService_shareSigningKey() {
        UUID accountId = UUID.randomUUID();

        assertThatCode(() -> jwtService.validateLocalToken(
                tokenFactory.localTokenForAccountId(accountId)))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> jwtService.validateLocalToken(
                tokenFactory.tokenSignedWithOtherSecret(
                        accountId.toString(), OTHER_SECRET, Duration.ofHours(1))))
                .isInstanceOf(JwtException.class);
    }

    /**
     * L'émetteur de la factory doit correspondre à {@code jwt.issuer}, sinon
     * les jetons qu'elle produit seraient refusés par
     * {@code validateLocalToken}.
     */
    @Test
    @DisplayName("L'émetteur de la factory est aligné sur jwt.issuer")
    void factory_issuerMatchesConfiguredIssuer() {
        assertThat(JwtTestTokenFactory.DEFAULT_ISSUER).isEqualTo(ISSUER);

        String token = tokenFactory.localTokenForAccountId(UUID.randomUUID());

        assertThatCode(() -> jwtService.validateLocalToken(token)).doesNotThrowAnyException();
    }
}
