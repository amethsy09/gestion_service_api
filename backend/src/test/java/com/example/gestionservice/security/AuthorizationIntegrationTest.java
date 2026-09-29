package com.example.gestionservice.security;

import com.example.gestionservice.AbstractIntegrationTest;
import com.example.gestionservice.entity.GestionAccount;
import com.example.gestionservice.enums.Role;
import com.example.gestionservice.repository.GestionAccountRepository;
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

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'autorisation — RBAC avec JWT réel.
 *
 * <p>À l'Étape 0, les comptes sont créés <b>directement en base</b>
 * ({@code gestion_account}), sans dépendre d'un jeton forgé à la main :
 * la construction du jeton est déléguée à
 * {@link JwtTestTokenFactory}, qui signe avec le même secret que
 * l'application.</p>
 *
 * <p>Les jetons utilisés ici sont au <b>format local</b>
 * ({@code sub = gestion_account.id}), seul format accepté depuis l'Étape 5.
 * Le compte doit exister et être actif en base : aucun jeton ne permet plus
 * de faire apparaître une identité.</p>
 */
@AutoConfigureMockMvc
@DisplayName("Authorization — Tests RBAC")
class AuthorizationIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired GestionAccountRepository gestionAccountRepository;

    @Value("${jwt.secret}")
    String jwtSecret;

    private JwtTestTokenFactory tokenFactory;

    private GestionAccount userAccount;
    private String userJwt;
    private GestionAccount adminAccount;
    private String adminJwt;

    @BeforeEach
    void setUp() {
        tokenFactory = new JwtTestTokenFactory(jwtSecret);

        // Comptes créés directement en base : aucun jeton forgé nécessaire.
        // Le jeton porte l'UUID du compte (sub), plus le téléphone.
        userAccount = createAccount(Role.ROLE_USER);
        adminAccount = createAccount(Role.ROLE_ADMIN);

        userJwt = tokenFactory.localTokenForAccount(userAccount);
        adminJwt = tokenFactory.localTokenForAccount(adminAccount);
    }

    // ================================================================
    //  Accès ADMIN
    // ================================================================

    @Test
    @DisplayName("USER → GET /api/v1/admin/service-requests → 403")
    void user_accessAdminEndpoint_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/service-requests")
                        .header("Authorization", "Bearer " + userJwt))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN → GET /api/v1/admin/service-requests → OK")
    void admin_accessAdminEndpoint_ok() throws Exception {
        mockMvc.perform(get("/api/v1/admin/service-requests")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk());
    }

    // ================================================================
    //  Accès en écriture aux ressources d'administration
    // ================================================================

    @Test
    @DisplayName("USER → POST /api/v1/prestations → 403")
    void user_accessPrestationEndpoint_forbidden() throws Exception {
        // Corps valide : PrestationRequest exige estimatedEndDate (non nulle, future).
        // Sans cela la validation Bean répond 400 avant que @PreAuthorize ne soit évalué.
        String body = objectMapper.writeValueAsString(Map.of(
                "serviceRequestId", UUID.randomUUID(),
                "responsibleId", UUID.randomUUID(),
                "name", "Prestation interdite",
                "estimatedEndDate", LocalDate.now().plusDays(30)
        ));

        mockMvc.perform(post("/api/v1/prestations")
                        .header("Authorization", "Bearer " + userJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("USER → POST /api/v1/services → 403")
    void user_createService_forbidden() throws Exception {
        mockMvc.perform(post("/api/v1/services")
                        .header("Authorization", "Bearer " + userJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Service interdit " + UUID.randomUUID(),
                                "basePrice", 1000))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN → POST /api/v1/services → OK (created)")
    void admin_createService_ok() throws Exception {
        mockMvc.perform(post("/api/v1/services")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Test Admin Service " + UUID.randomUUID(),
                                "basePrice", 1000))))
                .andExpect(status().isCreated());
    }

    // ================================================================
    //  Endpoints publics
    // ================================================================

    @Test
    @DisplayName("Sans JWT → GET /api/v1/services → OK (public)")
    void user_getServices_publicEndpoint_ok() throws Exception {
        mockMvc.perform(get("/api/v1/services"))
                .andExpect(status().isOk());
    }

    // ================================================================
    //  Contrat d'erreur d'authentification
    // ================================================================

    /**
     * Une requête anonyme (sans JWT) sur un endpoint protégé doit retourner
     * un JSON HTTP 401 via {@code RestAuthenticationEntryPoint}, et non un
     * 403 avec corps vide.
     */
    @Test
    @DisplayName("Sans JWT → POST /api/v1/service-requests → 401 JSON")
    void anonymous_protectedEndpoint_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/v1/service-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    /**
     * Étape 5 — l'auto-création de compte est <b>définitivement supprimée</b>.
     *
     * <p>Un jeton au format ancien ({@code sub = téléphone}) pour un téléphone
     * inconnu ne doit produire ni authentification, ni ligne en base.</p>
     */
    @Test
    @DisplayName("Ancien JWT sub=telephone → 401 et aucun compte créé")
    void legacyTelephoneJwt_rejectedAndNoAccountCreated() throws Exception {
        String unknownTelephone = JwtTestTokenFactory.uniqueTelephone();
        String jwt = tokenFactory.legacyTokenForTelephone(unknownTelephone);

        mockMvc.perform(post("/api/v1/service-requests")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "serviceId", "00000000-0000-0000-0000-000000000000",
                                "title", "Test"
                        ))))
                .andExpect(status().isUnauthorized());

        // Aucun compte n'a été créé pour ce téléphone.
        assertThat(gestionAccountRepository.findByTelephone(unknownTelephone)).isEmpty();
    }

    /**
     * Étape 5 — un UUID local valide mais sans compte correspondant est
     * rejeté, et ne crée rien.
     */
    @Test
    @DisplayName("JWT local avec UUID inexistant → 401 et aucun compte créé")
    void localJwt_unknownUuid_rejectedAndNoAccountCreated() throws Exception {
        UUID unknownId = UUID.randomUUID();
        long accountsBefore = gestionAccountRepository.count();
        String jwt = tokenFactory.localTokenForAccountId(unknownId);

        mockMvc.perform(get("/api/v1/service-requests/my")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isUnauthorized());

        // Le compte du test est intact et aucun compte n'a été ajouté.
        assertThat(gestionAccountRepository.count()).isEqualTo(accountsBefore);
        assertThat(gestionAccountRepository.findById(unknownId)).isEmpty();
    }

    // ================================================================
    //  Utilitaires
    // ================================================================

    /**
     * Crée un {@code gestion_account} directement en base et retourne le compte
     * (le jeton est ensuite dérivé de son UUID, plus de son téléphone).
     */
    private GestionAccount createAccount(Role role) {
        String telephone = JwtTestTokenFactory.uniqueTelephone();
        return gestionAccountRepository.saveAndFlush(GestionAccount.builder()
                .fullName("Test Account")
                .telephone(telephone)
                .email("test-" + telephone + "@example.com")
                .role(role)
                .active(true)
                .build());
    }
}
