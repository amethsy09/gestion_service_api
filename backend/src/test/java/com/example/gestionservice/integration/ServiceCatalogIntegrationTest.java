package com.example.gestionservice.integration;

import com.example.gestionservice.AbstractIntegrationTest;
import com.example.gestionservice.entity.ServiceCatalog;
import com.example.gestionservice.repository.ServiceCatalogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration — ServiceCatalog endpoints.
 * Utilise PostgreSQL réel via Testcontainers + migrations Flyway.
 */
@AutoConfigureMockMvc
@DisplayName("ServiceCatalog — Tests d'intégration")
class ServiceCatalogIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ServiceCatalogRepository repository;

    @Test
    @DisplayName("GET /api/v1/services — retourne la liste paginée")
    void getAll_returnsPagedResult() throws Exception {
        // Les données initiales sont insérées par V1 Flyway
        mockMvc.perform(get("/api/v1/services")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/services/active — retourne uniquement les services actifs")
    void getActive_returnsOnlyActiveServices() throws Exception {
        // Créer un service inactif
        ServiceCatalog inactive = ServiceCatalog.builder()
                .name("Service désactivé test")
                .basePrice(new BigDecimal("100"))
                .active(false)
                .build();
        repository.save(inactive);

        mockMvc.perform(get("/api/v1/services/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[*].active").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.is(true))));
    }

    @Test
    @DisplayName("GET /api/v1/services/{id} — 404 pour ID inconnu")
    void getById_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/services/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }
}
