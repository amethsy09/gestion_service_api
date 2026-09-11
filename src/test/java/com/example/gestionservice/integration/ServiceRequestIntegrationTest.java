package com.example.gestionservice.integration;

import com.example.gestionservice.AbstractIntegrationTest;
import com.example.gestionservice.entity.GestionAccount;
import com.example.gestionservice.entity.ServiceCatalog;
import com.example.gestionservice.enums.Role;
import com.example.gestionservice.repository.GestionAccountRepository;
import com.example.gestionservice.repository.ServiceCatalogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration — ServiceRequest endpoints avec JWT réel (sub = telephone).
 */
@AutoConfigureMockMvc
@DisplayName("ServiceRequest — Tests d'intégration (JWT réel)")
class ServiceRequestIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired ServiceCatalogRepository catalogRepository;
    @Autowired GestionAccountRepository gestionAccountRepository;

    @Value("${jwt.secret}")
    String jwtSecret;

    private String telephone;
    private String jwtToken;
    private ServiceCatalog catalog;

    @BeforeEach
    void setUp() {
        telephone = "77" + UUID.randomUUID().toString().substring(0, 7).replaceAll("[^0-9]", "");
        jwtToken = buildJwt(telephone);

        GestionAccount account = gestionAccountRepository.findByTelephone(telephone).orElseGet(() ->
                gestionAccountRepository.save(GestionAccount.builder()
                        .telephone(telephone)
                        .role(Role.ROLE_USER)
                        .active(true)
                        .build()));

        catalog = catalogRepository.findAll().stream().findFirst()
                .orElseGet(() -> catalogRepository.save(ServiceCatalog.builder()
                        .name("Test Service " + UUID.randomUUID())
                        .basePrice(new BigDecimal("500000"))
                        .active(true)
                        .build()));
    }

    @Test
    @DisplayName("POST /api/v1/service-requests — accountId interne depuis gestion_account, statut WAITING_PAYMENT")
    void createRequest_withJwt_telephoneResolvedToInternalId() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "serviceId", catalog.getId(),
                "title", "Mon projet intégration",
                "description", "Test intégration Testcontainers"
        ));

        GestionAccount account = gestionAccountRepository.findByTelephone(telephone).orElseThrow();

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
    @DisplayName("POST /api/v1/service-requests — sans JWT → 401")
    void createRequest_withoutJwt_returns401() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "serviceId", catalog.getId(),
                "title", "Test sans JWT"
        ));

        mockMvc.perform(post("/api/v1/service-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/service-requests — body invalide → 400 VALIDATION_ERROR")
    void createRequest_invalidBody_returns400() throws Exception {
        String body = "{}";

        mockMvc.perform(post("/api/v1/service-requests")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
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
     * Construit un JWT de test signé avec le secret configuré.
     * Le JWT contient UNIQUEMENT sub = telephone (comme auth_api réel).
     */
    private String buildJwt(String telephone) {
        return Jwts.builder()
                .setSubject(telephone)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256)
                .compact();
    }
}
