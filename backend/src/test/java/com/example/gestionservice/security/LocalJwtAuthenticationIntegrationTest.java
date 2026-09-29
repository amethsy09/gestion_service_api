package com.example.gestionservice.security;

import com.example.gestionservice.AbstractIntegrationTest;
import com.example.gestionservice.entity.GestionAccount;
import com.example.gestionservice.entity.ServiceCatalog;
import com.example.gestionservice.entity.ServiceRequest;
import com.example.gestionservice.enums.PaymentStatus;
import com.example.gestionservice.enums.Role;
import com.example.gestionservice.enums.ServiceRequestStatus;
import com.example.gestionservice.repository.GestionAccountRepository;
import com.example.gestionservice.repository.ServiceCatalogRepository;
import com.example.gestionservice.repository.ServiceRequestRepository;
import com.example.gestionservice.support.JwtTestTokenFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'intégration du pipeline d'authentification après l'Étape 5 —
 * cutover définitif de l'identité JWT {@code telephone → UUID}.
 *
 * <p>Chaque requête authentifiée passe désormais par :</p>
 * <pre>
 * JWT sub (UUID) → AccountSecurityResolver.resolve(accountId)
 *                → compte existant + active = true + rôle en base
 *                → JwtAuthenticationPrincipal → SecurityContext
 * </pre>
 *
 * <p>Ce qu'une requête doit subir :</p>
 * <ul>
 *   <li><b>acceptée</b> : JWT local valide dont le {@code sub} est l'UUID
 *       d'un compte existant et actif ;</li>
 *   <li><b>rejetée (401)</b> : UUID inexistant, compte désactivé, ancien jeton
 *       {@code sub = telephone}, {@code sub} non UUID, jeton expiré,
 *       mauvaise signature, émetteur incorrect.</li>
 * </ul>
 *
 * <p>Aucun de ces rejets ne doit créer de compte en base.</p>
 */
@AutoConfigureMockMvc
@DisplayName("Pipeline JWT UUID — Tests d'intégration (Étape 5)")
class LocalJwtAuthenticationIntegrationTest extends AbstractIntegrationTest {

    private static final String OTHER_SECRET = "another_wrong_secret_key_32_chars_min";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired GestionAccountRepository gestionAccountRepository;
    @Autowired ServiceCatalogRepository catalogRepository;
    @Autowired ServiceRequestRepository serviceRequestRepository;

    @Value("${jwt.secret}")
    String jwtSecret;

    private JwtTestTokenFactory tokenFactory;

    private GestionAccount userAccount;

    @BeforeEach
    void setUp() {
        tokenFactory = new JwtTestTokenFactory(jwtSecret);
        userAccount = createAccount(Role.ROLE_USER);
    }

    // ==================================================================
    //  Helpers
    // ==================================================================

    private GestionAccount createAccount(Role role) {
        return createAccount(role, true);
    }

    private GestionAccount createAccount(Role role, boolean active) {
        String telephone = JwtTestTokenFactory.uniqueTelephone();
        return gestionAccountRepository.saveAndFlush(GestionAccount.builder()
                .fullName("Test Account " + role)
                .telephone(telephone)
                .email("test-" + telephone + "@example.com")
                .role(role)
                .active(active)
                .build());
    }

    /** Appel protégé par défaut : /api/v1/service-requests/my. */
    private ResultActions callProtected(String jwt) throws Exception {
        return mockMvc.perform(get("/api/v1/service-requests/my")
                .header("Authorization", "Bearer " + jwt));
    }

    // ==================================================================
    //  JWT local valide
    // ==================================================================

    @Test
    @DisplayName("JWT local valide (sub = UUID du compte) → 200")
    void localJwt_validAccount_authenticated() throws Exception {
        String jwt = tokenFactory.localTokenForAccount(userAccount);

        callProtected(jwt)
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("JWT émis par JwtService.generateToken → 200")
    void tokenIssuedByProductionService_authenticated() throws Exception {
        // Le jeton réellement émis par l'application (login) est accepté.
        String jwt = new JwtService(jwtSecret, tokenFactory.DEFAULT_ISSUER, 3_600_000L)
                .generateToken(userAccount.getId());

        callProtected(jwt)
                .andExpect(status().isOk());
    }

    // ==================================================================
    //  Rejets — UUID
    // ==================================================================

    @Test
    @DisplayName("UUID inexistant → 401 et aucun compte créé")
    void unknownUuid_rejectedAndNoAccountCreated() throws Exception {
        long accountsBefore = gestionAccountRepository.count();

        String jwt = tokenFactory.localTokenForAccountId(UUID.randomUUID());

        callProtected(jwt)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("AUTHENTICATION_REQUIRED"));

        assertThat(gestionAccountRepository.count()).isEqualTo(accountsBefore);
    }

    @Test
    @DisplayName("Compte désactivé → 401")
    void disabledAccount_rejected() throws Exception {
        GestionAccount disabled = createAccount(Role.ROLE_USER, false);
        String jwt = tokenFactory.localTokenForAccount(disabled);

        callProtected(jwt)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    @DisplayName("Compte désactivé après obtention du jeton → 401 (validation à chaque requête)")
    void accountDisabledAfterTokenIssued_rejectedOnNextRequest() throws Exception {
        String jwt = tokenFactory.localTokenForAccount(userAccount);

        // Le jeton fonctionne tant que le compte est actif...
        callProtected(jwt).andExpect(status().isOk());

        // ...et ne fonctionne plus dès que le compte est désactivé.
        userAccount.setActive(false);
        gestionAccountRepository.saveAndFlush(userAccount);

        callProtected(jwt)
                .andExpect(status().isUnauthorized());
    }

    // ==================================================================
    //  Rejets — ancien format téléphone
    // ==================================================================

    @Test
    @DisplayName("Ancien JWT sub=telephone → 401 (compatibilité supprimée)")
    void legacyTelephoneJwt_rejected() throws Exception {
        String jwt = tokenFactory.legacyTokenForTelephone(userAccount.getTelephone());

        callProtected(jwt)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    @DisplayName("Ancien JWT sub=telephone d'un compte inexistant → 401, aucun compte créé")
    void legacyTelephoneJwt_unknown_rejectedAndNoAccountCreated() throws Exception {
        long accountsBefore = gestionAccountRepository.count();
        String unknownTelephone = JwtTestTokenFactory.uniqueTelephone();

        String jwt = tokenFactory.legacyTokenForTelephone(unknownTelephone);

        callProtected(jwt)
                .andExpect(status().isUnauthorized());

        assertThat(gestionAccountRepository.findByTelephone(unknownTelephone)).isEmpty();
        assertThat(gestionAccountRepository.count()).isEqualTo(accountsBefore);
    }

    @Test
    @DisplayName("sub = téléphone avec un émetteur valide → 401 (format non UUID refusé)")
    void telephoneSubjectWithValidIssuer_rejected() throws Exception {
        // Seule différence avec un jeton local : le sub n'est pas un UUID.
        String jwt = tokenFactory.localTokenWithSubject(userAccount.getTelephone());

        callProtected(jwt)
                .andExpect(status().isUnauthorized());
    }

    // ==================================================================
    //  Rejets — cryptographiques
    // ==================================================================

    @Test
    @DisplayName("JWT expiré → 401")
    void expiredJwt_rejected() throws Exception {
        String jwt = tokenFactory.localTokenForAccountId(userAccount.getId(), Duration.ofSeconds(-1));

        callProtected(jwt)
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Mauvaise signature → 401")
    void wrongSignature_rejected() throws Exception {
        String jwt = new JwtTestTokenFactory(OTHER_SECRET).localTokenForAccountId(userAccount.getId());

        callProtected(jwt)
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Émetteur incorrect → 401")
    void wrongIssuer_rejected() throws Exception {
        String jwt = tokenFactory.localTokenForAccountId(
                userAccount.getId(), "gestion-service-usurpe");

        callProtected(jwt)
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Subject vide → 401")
    void emptySubject_rejected() throws Exception {
        String jwt = tokenFactory.localTokenWithSubject(" ");

        callProtected(jwt)
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Aucun header Authorization → 401 (comportement inchangé)")
    void noAuthorizationHeader_rejected() throws Exception {
        mockMvc.perform(get("/api/v1/service-requests/my"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("AUTHENTICATION_REQUIRED"));
    }

    // ==================================================================
    //  Rôles lus en base
    // ==================================================================

    @Test
    @DisplayName("Rôle ADMIN relu en base → endpoint admin autorisé")
    void adminRole_readFromDatabase_authorised() throws Exception {
        GestionAccount admin = createAccount(Role.ROLE_ADMIN);
        String jwt = tokenFactory.localTokenForAccount(admin);

        mockMvc.perform(get("/api/v1/admin/service-requests")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Rôle USER → endpoint admin refusé (403, non 401)")
    void userRole_adminEndpoint_forbidden() throws Exception {
        String jwt = tokenFactory.localTokenForAccount(userAccount);

        mockMvc.perform(get("/api/v1/admin/service-requests")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Rôle RESPONSIBLE → ses endpoints restent autorisés")
    void responsibleRole_authorised() throws Exception {
        GestionAccount responsable = createAccount(Role.ROLE_RESPONSIBLE);
        String jwt = tokenFactory.localTokenForAccount(responsable);

        // GET /api/v1/prestations est réservé au RESPONSIBLE et à l'ADMIN.
        mockMvc.perform(get("/api/v1/prestations")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Changement de rôle en base → pris en compte dès la requête suivante")
    void roleChangedInDatabase_appliedImmediately() throws Exception {
        String jwt = tokenFactory.localTokenForAccount(userAccount);

        mockMvc.perform(get("/api/v1/admin/service-requests")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isForbidden());

        // Promotion en base : le rôle utilisé par @PreAuthorize vient de la base.
        userAccount.setRole(Role.ROLE_ADMIN);
        gestionAccountRepository.saveAndFlush(userAccount);

        mockMvc.perform(get("/api/v1/admin/service-requests")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());
    }

    // ==================================================================
    //  Ownership
    // ==================================================================

    @Test
    @DisplayName("Ownership — un compte ne peut pas lire la demande d'un autre compte")
    void ownership_otherUserRequest_notReadable() throws Exception {
        GestionAccount other = createAccount(Role.ROLE_USER);
        UUID otherRequestId = createServiceRequestFor(other);

        // Le propriétaire légitime y accède.
        mockMvc.perform(get("/api/v1/service-requests/" + otherRequestId)
                        .header("Authorization", "Bearer " + tokenFactory.localTokenForAccount(other)))
                .andExpect(status().isOk());

        // Un autre compte, lui aussi authentifié par UUID, n'obtient rien :
        // la recherche est filtrée par accountId (findByIdAndAccountId), donc
        // la demande d'autrui est comme inexistante. (Le refus explicite 403
        // est produit par le flux de paiement, cf. PaymentIdentityIntegrationTest.)
        mockMvc.perform(get("/api/v1/service-requests/" + otherRequestId)
                        .header("Authorization", "Bearer " + tokenFactory.localTokenForAccount(userAccount)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Ownership — /my ne renvoie que les demandes du compte authentifié")
    void ownership_myRequests_scopedToAuthenticatedAccount() throws Exception {
        UUID ownRequestId = createServiceRequestFor(userAccount);
        GestionAccount other = createAccount(Role.ROLE_USER);
        UUID otherRequestId = createServiceRequestFor(other);

        String body = mockMvc.perform(get("/api/v1/service-requests/my")
                        .header("Authorization", "Bearer " + tokenFactory.localTokenForAccount(userAccount)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains(ownRequestId.toString());
        assertThat(body).doesNotContain(otherRequestId.toString());
    }

    @Test
    @DisplayName("Ownership — un UUID inconnu n'hérite d'aucune demande existante")
    void ownership_unknownUuid_hasNoAccess() throws Exception {
        UUID otherRequestId = createServiceRequestFor(userAccount);

        mockMvc.perform(get("/api/v1/service-requests/" + otherRequestId)
                        .header("Authorization", "Bearer " + tokenFactory.localTokenForAccountId(UUID.randomUUID())))
                .andExpect(status().isUnauthorized());
    }

    // ==================================================================
    //  Endpoints publics — comportement inchangé
    // ==================================================================

    @Test
    @DisplayName("POST /api/v1/auth/register reste accessible sans JWT")
    void registerEndpoint_remainsPublic() throws Exception {
        String telephone = JwtTestTokenFactory.uniqueTelephone();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "Nouvel Utilisateur",
                                "telephone", telephone,
                                "email", "nouveau-" + telephone + "@example.com",
                                "password", "motdepasse123"
                        ))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login reste accessible sans JWT")
    void loginEndpoint_remainsPublic() throws Exception {
        String telephone = JwtTestTokenFactory.uniqueTelephone();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "telephone", telephone,
                                "password", "motdepasse-inexistant"
                        ))))
                // 401 : l'endpoint est bien atteignable sans JWT, seul
                // l'identifiant est refusé.
                .andExpect(status().isUnauthorized());
    }

    // ==================================================================
    //  Utilitaires
    // ==================================================================

    /**
     * Crée une demande de service rattachée au compte donné, directement en
     * base : elle sert de cible aux tests d'ownership.
     */
    private UUID createServiceRequestFor(GestionAccount account) {
        // service_catalog_id est NOT NULL : chaque demande est rattachée à
        // un catalogue créé pour ce test.
        ServiceCatalog catalog = catalogRepository.saveAndFlush(ServiceCatalog.builder()
                .name("Service d'ownership " + UUID.randomUUID())
                .basePrice(new BigDecimal("100000"))
                .active(true)
                .build());

        return serviceRequestRepository.saveAndFlush(
                        ServiceRequest.builder()
                                .accountId(account.getId())
                                .serviceCatalog(catalog)
                                .title("Demande " + UUID.randomUUID())
                                .description("Demande de test d'ownership")
                                .amount(new BigDecimal("100000"))
                                .status(ServiceRequestStatus.WAITING_PAYMENT)
                                .paymentStatus(PaymentStatus.PENDING)
                                .build())
                .getId();
    }
}
