package com.example.gestionservice.integration;

import com.example.gestionservice.AbstractIntegrationTest;
import com.example.gestionservice.entity.GestionAccount;
import com.example.gestionservice.entity.ServiceCatalog;
import com.example.gestionservice.enums.Role;
import com.example.gestionservice.repository.GestionAccountRepository;
import com.example.gestionservice.repository.ServiceCatalogRepository;
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

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
/**
 * Tests d'intégration — endpoints ServiceRequest avec JWT réel.
 *
 * <p>Le compte de test est créé <b>directement en base</b> et le jeton est
 * produit par {@link JwtTestTokenFactory} (même secret que l'application) au
 * format local {@code sub = gestion_account.id}, seul format accepté depuis
 * l'Étape 5.</p>
 */
@AutoConfigureMockMvc
@DisplayName("ServiceRequest — Tests d'intégration (JWT réel)")
class ServiceRequestIntegrationTest extends AbstractIntegrationTest {

    /** Montant du service de test — également le montant attendu sur la demande. */
    private static final String TEST_SERVICE_PRICE = "500000";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired ServiceCatalogRepository catalogRepository;
    @Autowired GestionAccountRepository gestionAccountRepository;

    @Value("${jwt.secret}")
    String jwtSecret;

    private JwtTestTokenFactory tokenFactory;

    private String telephone;
    private String jwtToken;
    private GestionAccount account;
    private ServiceCatalog catalog;

    @BeforeEach
    void setUp() {
        tokenFactory = new JwtTestTokenFactory(jwtSecret);

        // Compte créé directement en base : le test ne dépend plus d'un jeton
        // forgé pour établir l'identité.
        telephone = JwtTestTokenFactory.uniqueTelephone();
        account = gestionAccountRepository.saveAndFlush(GestionAccount.builder()
                .fullName("Test User")
                .telephone(telephone)
                .email("test-" + telephone + "@example.com")
                .role(Role.ROLE_USER)
                .active(true)
                .build());

        // Format local : sub = gestion_account.id (UUID).
        jwtToken = tokenFactory.localTokenForAccount(account);

        // Catalogue dédié à ce test : ne pas réutiliser une entrée existante,
        // dont le prix ferait échouer l'assertion sur le montant de la demande.
        catalog = ServiceCatalog.builder()
                .name("Test Service " + UUID.randomUUID())
                .basePrice(new BigDecimal(TEST_SERVICE_PRICE))
                .active(true)
                .build();
        catalog = catalogRepository.save(catalog);
    }

    @Test
    @DisplayName("POST /api/v1/service-requests — accountId interne depuis gestion_account, statut WAITING_PAYMENT")
    void createRequest_withLocalJwt_accountIdResolvedFromUuidSubject() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "serviceId", catalog.getId(),
                "title", "Mon projet intégration",
                "description", "Test intégration Testcontainers"
        ));

        mockMvc.perform(post("/api/v1/service-requests")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accountId").value(account.getId().toString()))
                .andExpect(jsonPath("$.data.status").value("WAITING_PAYMENT"))
                .andExpect(jsonPath("$.data.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.amount").value(500000));
    }

    @Test
    @DisplayName("GET /api/v1/service-requests/my — filtre sur accountId interne")
    void getMyRequests_filteredByInternalAccountId() throws Exception {
        mockMvc.perform(get("/api/v1/service-requests/my")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("POST /api/v1/service-requests — body invalide → 400 VALIDATION_ERROR")
    void createRequest_invalidBody_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/service-requests")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/services — public, sans JWT → OK")
    void getServices_withoutJwt_returnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/services"))
                .andExpect(status().isOk());
    }

    /**
     * Une requête anonyme (sans JWT) sur un endpoint protégé retourne un
     * JSON HTTP 401 via {@code RestAuthenticationEntryPoint}.
     *
     * <p>Ce comportement a été corrigé par l'ajout de
     * {@code RestAuthenticationEntryPoint} et {@code RestAccessDeniedHandler}
     * dans {@code SecurityConfig}.</p>
     */
    @Test
    @DisplayName("GET /api/v1/service-requests/my — sans JWT → 401 JSON (auth requis)")
    void getMyRequests_withoutJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/service-requests/my"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    /**
     * Une requête anonyme (sans JWT) sur un endpoint protégé en écriture
     * retourne un JSON HTTP 401 via {@code RestAuthenticationEntryPoint}.
     *
     * <p>Ce comportement a été corrigé par l'ajout de
     * {@code RestAuthenticationEntryPoint} et {@code RestAccessDeniedHandler}
     * dans {@code SecurityConfig}.</p>
     */
    @Test
    @DisplayName("POST /api/v1/service-requests — sans JWT → 401 JSON (auth requis)")
    void createRequest_withoutJwt_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/service-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}
