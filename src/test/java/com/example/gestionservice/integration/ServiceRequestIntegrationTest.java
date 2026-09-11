package com.example.gestionservice.integration;

import com.example.gestionservice.AbstractIntegrationTest;
import com.example.gestionservice.entity.ServiceCatalog;
import com.example.gestionservice.repository.ServiceCatalogRepository;
import com.example.gestionservice.security.JwtService;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration — ServiceRequest endpoints avec JWT.
 */
@AutoConfigureMockMvc
@DisplayName("ServiceRequest — Tests d'intégration")
class ServiceRequestIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired ServiceCatalogRepository catalogRepository;

    @Value("${jwt.secret}")
    String jwtSecret;

    private UUID accountId;
    private String jwtToken;
    private ServiceCatalog catalog;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        jwtToken = buildJwt(accountId, List.of("USER"));

        catalog = catalogRepository.findAll().stream().findFirst()
                .orElseGet(() -> catalogRepository.save(ServiceCatalog.builder()
                        .name("Test Service " + UUID.randomUUID())
                        .basePrice(new BigDecimal("500000"))
                        .active(true)
                        .build()));
    }

    @Test
    @DisplayName("POST /api/v1/service-requests — accountId extrait du JWT, statut WAITING_PAYMENT")
    void createRequest_withJwt_accountIdFromToken() throws Exception {
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
                .andExpect(jsonPath("$.data.accountId").value(accountId.toString()))
                .andExpect(jsonPath("$.data.status").value("WAITING_PAYMENT"))
                .andExpect(jsonPath("$.data.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.amount").value(500000));
    }

    @Test
    @DisplayName("GET /api/v1/service-requests/my — filtre sur accountId JWT")
    void getMyRequests_filteredByAccountId() throws Exception {
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
        // Pas de serviceId ni de title
        String body = "{}";

        mockMvc.perform(post("/api/v1/service-requests")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    /** Construit un JWT de test signé avec le secret configuré. */
    private String buildJwt(UUID accountId, List<String> roles) {
        return Jwts.builder()
                .setSubject(accountId.toString())
                .claim("accountId", accountId.toString())
                .claim("roles", roles)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256)
                .compact();
    }
}
