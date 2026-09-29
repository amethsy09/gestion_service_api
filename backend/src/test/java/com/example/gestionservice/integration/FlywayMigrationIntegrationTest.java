package com.example.gestionservice.integration;

import com.example.gestionservice.AbstractIntegrationTest;
import com.example.gestionservice.repository.*;
import com.example.gestionservice.entity.GestionAccount;
import com.example.gestionservice.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    @Autowired GestionAccountRepository gestionAccountRepository;

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
    @DisplayName("V10 — gestion_account : création et contraintes")
    void v10_gestionAccount_tableConstraints() {
        GestionAccount account = GestionAccount.builder()
                .fullName("Test User")
                .telephone("771234567")
                .email("test.user@example.com")
                .role(Role.ROLE_USER)
                .active(true)
                .build();
        gestionAccountRepository.save(account);

        assertThat(account.getId()).isNotNull();
        assertThat(account.getTelephone()).isEqualTo("771234567");
        assertThat(account.getFullName()).isEqualTo("Test User");
        assertThat(account.getEmail()).isEqualTo("test.user@example.com");
        assertThat(account.getRole()).isEqualTo(Role.ROLE_USER);

        // Test unicité telephone
        GestionAccount duplicate = GestionAccount.builder()
                .fullName("Duplicate")
                .telephone("771234567")
                .email("dup@example.com")
                .role(Role.ROLE_ADMIN)
                .active(true)
                .build();
        assertThatThrownBy(() -> gestionAccountRepository.saveAndFlush(duplicate))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("V1-V10 — toutes les tables existent et sont requêtables")
    void allTables_existAndQueryable() {
        assertThat(serviceCatalogRepository.findAll()).isNotNull();
        assertThat(specialtyRepository.findAll()).isNotNull();
        assertThat(serviceRequestRepository.findAll()).isNotNull();
        assertThat(prestationRepository.findAll()).isNotNull();
        assertThat(resourceRepository.findAll()).isNotNull();
        assertThat(taskRepository.findAll()).isNotNull();
        assertThat(paymentAttemptRepository.findAll()).isNotNull();
        assertThat(gestionAccountRepository.findAll()).isNotNull();
    }

    @Test
    @DisplayName("service_catalog — contrainte basePrice > 0")
    void serviceCatalog_activeSuffix_allPricesPositive() {
        serviceCatalogRepository.findAll().forEach(s ->
                assertThat(s.getBasePrice().signum()).isEqualTo(1));
    }
}
