package com.example.gestionservice.security;

import com.example.gestionservice.entity.GestionAccount;
import com.example.gestionservice.enums.Role;
import com.example.gestionservice.repository.GestionAccountRepository;
import com.example.gestionservice.service.impl.PrestationServiceImpl;
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

import com.example.gestionservice.AbstractIntegrationTest;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests d'autorisation — RBAC avec JWT réel (sub = telephone).
 * Vérifie que les rôles locaux sont respectés depuis gestion_account.
 */
@AutoConfigureMockMvc
@DisplayName("Authorization — Tests RBAC")
class AuthorizationIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired GestionAccountRepository gestionAccountRepository;

    @Value("${jwt.secret}")
    String jwtSecret;

    private String userTelephone;
    private String userJwt;
    private String adminTelephone;
    private String adminJwt;

    @BeforeEach
    void setUp() {
        userTelephone = "77" + UUID.randomUUID().toString().substring(0, 7).replaceAll("[^0-9]", "");
        userJwt = buildJwt(userTelephone);

        adminTelephone = "77" + UUID.randomUUID().toString().substring(0, 7).replaceAll("[^0-9]", "");
        adminJwt = buildJwt(adminTelephone);

        // Create gestion_account for USER
        gestionAccountRepository.save(GestionAccount.builder()
                .telephone(userTelephone)
                .role(Role.ROLE_USER)
                .active(true)
                .build());

        // Create gestion_account for ADMIN
        gestionAccountRepository.save(GestionAccount.builder()
                .telephone(adminTelephone)
                .role(Role.ROLE_ADMIN)
                .active(true)
                .build());
    }

    @Test
    @DisplayName("USER → GET /api/v1/admin/service-requests → 403")
    void user_accessAdminEndpoint_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/service-requests")
                        .header("Authorization", "Bearer " + userJwt))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("USER → POST /api/v1/prestations → 403")
    void user_accessPrestationEndpoint_forbidden() throws Exception {
        mockMvc.perform(post("/api/v1/prestations")
                        .header("Authorization", "Bearer " + userJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serviceRequestId\":\"" + UUID.randomUUID() + "\",\"responsibleId\":\"" + UUID.randomUUID() + "\",\"name\":\"Test\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("USER → POST /api/v1/services → 403")
    void user_createService_forbidden() throws Exception {
        mockMvc.perform(post("/api/v1/services")
                        .header("Authorization", "Bearer " + userJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test\",\"basePrice\":1000}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN → GET /api/v1/admin/service-requests → OK")
    void admin_accessAdminEndpoint_ok() throws Exception {
        mockMvc.perform(get("/api/v1/admin/service-requests")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ADMIN → POST /api/v1/services → OK (created)")
    void admin_createService_ok() throws Exception {
        mockMvc.perform(post("/api/v1/services")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test Admin Service " + UUID.randomUUID() + "\",\"basePrice\":1000}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("USER → GET /api/v1/services → OK (public)")
    void user_getServices_publicEndpoint_ok() throws Exception {
        mockMvc.perform(get("/api/v1/services"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("UTILISATEUR INCONNU → POST /api/v1/service-requests → création gestion_account ROLE_USER")
    void unknownTelephone_createsAccountAndAuthOk() throws Exception {
        String newTelephone = "77" + UUID.randomUUID().toString().substring(0, 7).replaceAll("[^0-9]", "");
        String jwt = buildJwt(newTelephone);

        mockMvc.perform(post("/api/v1/service-requests")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper().writeValueAsString(Map.of(
                                "serviceId", "00000000-0000-0000-0000-000000000000",
                                "title", "Test"
                        ))))
                .andExpect(result -> {
                    // Peut échouer sur 404 (service inexistant) ou 201, mais jamais 401
                    int status = result.getResponse().getStatus();
                    assert status != 401 : "Authentication should not fail";
                });

        GestionAccount account = gestionAccountRepository.findByTelephone(newTelephone).orElse(null);
        assertThat(account).isNotNull();
        assertThat(account.getRole()).isEqualTo(Role.ROLE_USER);
    }

    private String buildJwt(String telephone) {
        return Jwts.builder()
                .setSubject(telephone)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256)
                .compact();
    }

    private com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
        return new com.fasterxml.jackson.databind.ObjectMapper();
    }
}
