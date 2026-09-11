package com.example.gestionservice.integration;

import com.example.gestionservice.AbstractIntegrationTest;
import com.example.gestionservice.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie que les migrations Flyway V1-V9 s'exécutent correctement
 * et que les données initiales sont présentes.
 */
@AutoConfigureMockMvc
@DisplayName("Flyway Migrations — Tests d'intégration")
class FlywayMigrationIntegrationTest extends AbstractIntegrationTest {

    @Autowired ServiceCatalogRepository serviceCatalogRepository;
    @Autowired SpecialtyRepository specialtyRepository;
    @Autowired ServiceRequestRepository serviceRequestRepository;
    @Autowired PrestationRepository prestationRepository;
    @Autowired ResourceRepository resourceRepository;
    @Autowired TaskRepository taskRepository;
    @Autowired PaymentAttemptRepository paymentAttemptRepository;

    @Test
    @DisplayName("V1 — service_catalog : données initiales présentes")
    void v1_serviceCatalog_initialDataPresent() {
        long count = serviceCatalogRepository.count();
        assertThat(count).isGreaterThanOrEqualTo(6);
    }

    @Test
    @DisplayName("V5 — specialties : données initiales présentes")
    void v5_specialties_initialDataPresent() {
        long count = specialtyRepository.count();
        assertThat(count).isGreaterThanOrEqualTo(7);
    }

    @Test
    @DisplayName("V1-V9 — toutes les tables existent et sont requêtables")
    void allTables_existAndQueryable() {
        assertThat(serviceCatalogRepository.findAll()).isNotNull();
        assertThat(specialtyRepository.findAll()).isNotNull();
        assertThat(serviceRequestRepository.findAll()).isNotNull();
        assertThat(prestationRepository.findAll()).isNotNull();
        assertThat(resourceRepository.findAll()).isNotNull();
        assertThat(taskRepository.findAll()).isNotNull();
        assertThat(paymentAttemptRepository.findAll()).isNotNull();
    }

    @Test
    @DisplayName("service_catalog — contrainte basePrice > 0")
    void serviceCatalog_activeSuffix_allPricesPositive() {
        serviceCatalogRepository.findAll().forEach(s ->
                assertThat(s.getBasePrice().signum()).isEqualTo(1));
    }
}
