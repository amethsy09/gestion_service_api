package com.example.gestionservice.bootstrap;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.gestionservice.AbstractIntegrationTest;
import com.example.gestionservice.entity.GestionAccount;
import com.example.gestionservice.enums.Role;
import com.example.gestionservice.exception.AdminSeedConflictException;
import com.example.gestionservice.repository.GestionAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests d'intégration de l'Étape 4 — Admin Seeder.
 *
 * <p>Couvre la création du compte ADMIN initial, le rôle et l'activation,
 * le hachage BCrypt, l'absence de mot de passe en clair, l'idempotence
 * (aucun second ADMIN au redémarrage), la non-modification d'un ADMIN
 * existant (actif ou désactivé, mot de passe compris), le comportement
 * explicite en cas de conflit téléphone/email, et le seeding désactivé.</p>
 *
 * <p>Le seeder est appelé via {@code run(ApplicationArguments)} — le point
 * d'entrée réel de Spring au démarrage — et non via une méthode interne.</p>
 */
@TestPropertySource(properties = {
        "admin.seed.enabled=true",
        "admin.seed.full-name=Administrateur Seed",
        // Volontairement en format international et en casse mixte :
        // le seeder doit normaliser avant de stocker.
        "admin.seed.telephone=+221 77 100 00 01",
        "admin.seed.email=Admin.Seed@Example.COM",
        "admin.seed.password=motdepasse-admin-2026"
})
@DisplayName("AdminSeeder — Tests d'intégration (Étape 4)")
class AdminSeederIntegrationTest extends AbstractIntegrationTest {

    private static final String SEED_PASSWORD = "motdepasse-admin-2026";
    private static final String CANONICAL_TELEPHONE = "771000001";
    private static final String NORMALISED_EMAIL = "admin.seed@example.com";
    private static final String NORMALISED_FULL_NAME = "Administrateur Seed";

    @Autowired AdminSeeder adminSeeder;
    @Autowired AdminSeedProperties adminSeedProperties;
    @Autowired GestionAccountRepository gestionAccountRepository;
    @Autowired PasswordEncoder passwordEncoder;

    /** Appelle uniquement le point d'entrée de démarrage. */
    private void runStartupSeeder() {
        adminSeeder.run(new DefaultApplicationArguments());
    }

    @BeforeEach
    void resetState() {
        // État déterministe : aucun compte ne doit pré-exister entre les tests.
        // (Le seeder a déjà tourné au démarrage du contexte, on repart de zéro.)
        gestionAccountRepository.deleteAll();
        adminSeedProperties.setEnabled(true);
        adminSeedProperties.setFullName("Administrateur Seed");
        adminSeedProperties.setTelephone("+221 77 100 00 01");
        adminSeedProperties.setEmail("Admin.Seed@Example.COM");
        adminSeedProperties.setPassword(SEED_PASSWORD);
    }

    // ==================================================================
    //  Helpers
    // ==================================================================

    private GestionAccount persist(GestionAccount account) {
        return gestionAccountRepository.saveAndFlush(account);
    }

    private long countByRole(Role role) {
        return gestionAccountRepository.findAll().stream()
                .filter(a -> a.getRole() == role)
                .count();
    }

    private GestionAccount adminByTelephone() {
        return gestionAccountRepository.findByTelephone(CANONICAL_TELEPHONE).orElseThrow();
    }

    // ==================================================================
    //  CRÉATION
    // ==================================================================

    @Test
    @DisplayName("Aucun ADMIN existant → le compte ADMIN est créé")
    void seed_noAdmin_createsAccount() {
        runStartupSeeder();

        assertThat(gestionAccountRepository.count()).isEqualTo(1);
        GestionAccount admin = adminByTelephone();
        assertThat(admin.getFullName()).isEqualTo(NORMALISED_FULL_NAME);
        assertThat(admin.getTelephone()).isEqualTo(CANONICAL_TELEPHONE);
        assertThat(admin.getEmail()).isEqualTo(NORMALISED_EMAIL);
    }

    @Test
    @DisplayName("Le compte créé a le rôle ROLE_ADMIN")
    void seed_createsAccountWithRoleAdmin() {
        runStartupSeeder();

        assertThat(adminByTelephone().getRole()).isEqualTo(Role.ROLE_ADMIN);
        assertThat(countByRole(Role.ROLE_ADMIN)).isEqualTo(1);
    }

    @Test
    @DisplayName("Le compte créé a active = true")
    void seed_createsAccountWithActiveTrue() {
        runStartupSeeder();

        assertThat(adminByTelephone().getActive()).isTrue();
    }

    @Test
    @DisplayName("Aucun compte ROLE_USER n'est créé")
    void seed_createsNoUserAccount() {
        runStartupSeeder();

        assertThat(countByRole(Role.ROLE_USER)).isZero();
        assertThat(countByRole(Role.ROLE_RESPONSIBLE)).isZero();
    }

    @Test
    @DisplayName("Téléphone et email configurés sont normalisés avant stockage")
    void seed_normalisesTelephoneAndEmail() {
        runStartupSeeder();

        GestionAccount admin = adminByTelephone();
        assertThat(admin.getTelephone()).isEqualTo(CANONICAL_TELEPHONE);
        assertThat(admin.getEmail()).isEqualTo(NORMALISED_EMAIL);
        assertThat(gestionAccountRepository.findByEmail(NORMALISED_EMAIL)).isPresent();
    }

    // ==================================================================
    //  MOT DE PASSE
    // ==================================================================

    @Test
    @DisplayName("Mot de passe stocké sous forme de hash BCrypt")
    void seed_passwordStoredAsBcrypt() {
        runStartupSeeder();

        String stored = adminByTelephone().getPassword();
        assertThat(stored).startsWith("$2a$12$");
        assertThat(stored).hasSize(60);
        assertThat(passwordEncoder.matches(SEED_PASSWORD, stored)).isTrue();
        assertThat(passwordEncoder.matches("mauvais-mot-de-passe", stored)).isFalse();
    }

    @Test
    @DisplayName("Mot de passe JAMAIS stocké en clair")
    void seed_passwordNeverStoredInClear() {
        runStartupSeeder();

        String stored = adminByTelephone().getPassword();
        assertThat(stored).isNotEqualTo(SEED_PASSWORD);
        assertThat(stored).doesNotContain(SEED_PASSWORD);
        assertThat(stored).contains("$2");
    }

    @Test
    @DisplayName("Mot de passe absent de la configuration → échec explicite, aucun compte créé")
    void seed_missingPassword_failsWithoutCreatingAccount() {
        adminSeedProperties.setPassword("  ");

        assertThatThrownBy(this::runStartupSeeder)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("admin.seed.password");

        assertThat(gestionAccountRepository.count()).isZero();
    }

    // ==================================================================
    //  IDEMPOTENCE
    // ==================================================================

    @Test
    @DisplayName("Redémarrage : un second passage ne crée pas de second ADMIN")
    void seed_secondRun_createsNoSecondAdmin() {
        runStartupSeeder();
        GestionAccount firstAdmin = adminByTelephone();

        runStartupSeeder();

        assertThat(gestionAccountRepository.count()).isEqualTo(1);
        assertThat(countByRole(Role.ROLE_ADMIN)).isEqualTo(1);
        assertThat(adminByTelephone().getId()).isEqualTo(firstAdmin.getId());
    }

    @Test
    @DisplayName("Idempotence : seed() renvoie SKIPPED_ADMIN_ALREADY_EXISTS au second passage")
    void seed_secondRun_reportsAlreadyExists() {
        assertThat(adminSeeder.seed()).isEqualTo(AdminSeeder.SeedResult.CREATED);
        assertThat(adminSeeder.seed()).isEqualTo(AdminSeeder.SeedResult.SKIPPED_ADMIN_ALREADY_EXISTS);
        assertThat(gestionAccountRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Idempotence : le hash n'est pas recalculé au second passage")
    void seed_secondRun_doesNotRehashPassword() {
        runStartupSeeder();
        String hashAfterFirstRun = adminByTelephone().getPassword();

        runStartupSeeder();

        assertThat(adminByTelephone().getPassword()).isEqualTo(hashAfterFirstRun);
    }

    // ==================================================================
    //  ADMIN EXISTANT — AUCUNE MODIFICATION
    // ==================================================================

    @Test
    @DisplayName("ADMIN existant actif → aucune modification (nom, email, téléphone, rôle)")
    void seed_existingActiveAdmin_notModified() {
        String originalHash = passwordEncoder.encode("un-mot-de-passe-existant");
        persist(GestionAccount.builder()
                .fullName("Directeur existant")
                .telephone(CANONICAL_TELEPHONE)
                .email(NORMALISED_EMAIL)
                .password(originalHash)
                .role(Role.ROLE_ADMIN)
                .active(true)
                .build());

        runStartupSeeder();

        GestionAccount admin = adminByTelephone();
        assertThat(gestionAccountRepository.count()).isEqualTo(1);
        assertThat(admin.getFullName()).isEqualTo("Directeur existant");
        assertThat(admin.getEmail()).isEqualTo(NORMALISED_EMAIL);
        assertThat(admin.getRole()).isEqualTo(Role.ROLE_ADMIN);
        assertThat(admin.getActive()).isTrue();
        assertThat(admin.getPassword()).isEqualTo(originalHash);
    }

    @Test
    @DisplayName("ADMIN existant désactivé → n'est PAS réactivé")
    void seed_existingInactiveAdmin_notReactivated() {
        persist(GestionAccount.builder()
                .fullName("Administrateur désactivé")
                .telephone(CANONICAL_TELEPHONE)
                .email(NORMALISED_EMAIL)
                .password(passwordEncoder.encode("ancien-mot-de-passe"))
                .role(Role.ROLE_ADMIN)
                .active(false)
                .build());

        runStartupSeeder();

        GestionAccount admin = adminByTelephone();
        assertThat(gestionAccountRepository.count()).isEqualTo(1);
        assertThat(admin.getActive()).isFalse();
        assertThat(admin.getFullName()).isEqualTo("Administrateur désactivé");
    }

    @Test
    @DisplayName("Mot de passe d'un ADMIN existant → n'est pas remplacé")
    void seed_existingAdminPassword_notReplaced() {
        String originalPassword = "ancien-mot-de-passe";
        String originalHash = passwordEncoder.encode(originalPassword);
        persist(GestionAccount.builder()
                .fullName("Directeur existant")
                .telephone(CANONICAL_TELEPHONE)
                .email(NORMALISED_EMAIL)
                .password(originalHash)
                .role(Role.ROLE_ADMIN)
                .active(true)
                .build());

        runStartupSeeder();

        String stored = adminByTelephone().getPassword();
        assertThat(stored).isEqualTo(originalHash);
        assertThat(passwordEncoder.matches(originalPassword, stored)).isTrue();
        // Le mot de passe de la configuration n'a pas été appliqué.
        assertThat(passwordEncoder.matches(SEED_PASSWORD, stored)).isFalse();
    }

    @Test
    @DisplayName("ADMIN existant avec un autre téléphone → aucun second compte créé")
    void seed_existingAdminWithOtherTelephone_notDuplicated() {
        persist(GestionAccount.builder()
                .fullName("Autre administrateur")
                .telephone("779999999")
                .email("autre.admin@example.com")
                .password(passwordEncoder.encode("ancien-mot-de-passe"))
                .role(Role.ROLE_ADMIN)
                .active(true)
                .build());

        runStartupSeeder();

        assertThat(countByRole(Role.ROLE_ADMIN)).isEqualTo(1);
        assertThat(gestionAccountRepository.findByTelephone("779999999")).isPresent();
        assertThat(gestionAccountRepository.findByTelephone(CANONICAL_TELEPHONE)).isEmpty();
    }

    // ==================================================================
    //  CONFLIT TÉLÉPHONE / EMAIL
    // ==================================================================

    @Test
    @DisplayName("Téléphone déjà pris par un autre compte → échec explicite, compte intact")
    void seed_telephoneConflict_failsAndLeavesAccountUntouched() {
        persist(GestionAccount.builder()
                .fullName("Utilisateur existant")
                .telephone(CANONICAL_TELEPHONE)
                .email("utilisateur@example.com")
                .password(passwordEncoder.encode("motdepasse-utilisateur"))
                .role(Role.ROLE_USER)
                .active(true)
                .build());

        assertThatThrownBy(this::runStartupSeeder)
                .isInstanceOf(AdminSeedConflictException.class)
                .hasMessageContaining("admin.seed.telephone");

        // Le compte en conflit n'est ni écrasé, ni modifié, ni promu ADMIN.
        assertThat(gestionAccountRepository.count()).isEqualTo(1);
        GestionAccount user = adminByTelephone();
        assertThat(user.getRole()).isEqualTo(Role.ROLE_USER);
        assertThat(user.getActive()).isTrue();
        assertThat(user.getFullName()).isEqualTo("Utilisateur existant");
        assertThat(user.getEmail()).isEqualTo("utilisateur@example.com");
        assertThat(countByRole(Role.ROLE_ADMIN)).isZero();
    }

    @Test
    @DisplayName("Email déjà pris par un autre compte → échec explicite, compte intact")
    void seed_emailConflict_failsAndLeavesAccountUntouched() {
        persist(GestionAccount.builder()
                .fullName("Utilisateur existant")
                .telephone("778888888")
                .email(NORMALISED_EMAIL)
                .password(passwordEncoder.encode("motdepasse-utilisateur"))
                .role(Role.ROLE_USER)
                .active(true)
                .build());

        assertThatThrownBy(this::runStartupSeeder)
                .isInstanceOf(AdminSeedConflictException.class)
                .hasMessageContaining("admin.seed.email");

        assertThat(gestionAccountRepository.count()).isEqualTo(1);
        GestionAccount user = gestionAccountRepository.findByEmail(NORMALISED_EMAIL).orElseThrow();
        assertThat(user.getRole()).isEqualTo(Role.ROLE_USER);
        assertThat(user.getTelephone()).isEqualTo("778888888");
        assertThat(user.getFullName()).isEqualTo("Utilisateur existant");
        assertThat(countByRole(Role.ROLE_ADMIN)).isZero();
    }

    @Test
    @DisplayName("Conflit ignoré si un ADMIN existe déjà (priorité à l'idempotence)")
    void seed_conflictButAdminExists_doesNotFail() {
        persist(GestionAccount.builder()
                .fullName("Administrateur existant")
                .telephone(CANONICAL_TELEPHONE)
                .email(NORMALISED_EMAIL)
                .password(passwordEncoder.encode("ancien-mot-de-passe"))
                .role(Role.ROLE_ADMIN)
                .active(true)
                .build());

        // Aucune exception : la présence d'un ADMIN suffit à ne rien faire.
        assertThat(adminSeeder.seed()).isEqualTo(AdminSeeder.SeedResult.SKIPPED_ADMIN_ALREADY_EXISTS);
        assertThat(gestionAccountRepository.count()).isEqualTo(1);
    }

    // ==================================================================
    //  SEEDING DÉSACTIVÉ
    // ==================================================================

    @Test
    @DisplayName("ADMIN_SEED_ENABLED=false → aucun seeding")
    void seed_disabled_createsNothing() {
        adminSeedProperties.setEnabled(false);

        assertThat(adminSeeder.seed()).isEqualTo(AdminSeeder.SeedResult.SKIPPED_DISABLED);

        assertThat(gestionAccountRepository.count()).isZero();
        assertThat(countByRole(Role.ROLE_ADMIN)).isZero();
    }

    @Test
    @DisplayName("Seeding désactivé → aucun compte n'est modifié non plus")
    void seed_disabled_leavesExistingAccountsUntouched() {
        String originalHash = passwordEncoder.encode("motdepasse-utilisateur");
        persist(GestionAccount.builder()
                .fullName("Administrateur existant")
                .telephone("779999999")
                .email("autre.admin@example.com")
                .password(originalHash)
                .role(Role.ROLE_ADMIN)
                .active(false)
                .build());

        adminSeedProperties.setEnabled(false);
        runStartupSeeder();

        GestionAccount admin = gestionAccountRepository.findByTelephone("779999999").orElseThrow();
        assertThat(admin.getPassword()).isEqualTo(originalHash);
        assertThat(admin.getActive()).isFalse();
    }

    // ==================================================================
    //  SÉCURITÉ
    // ==================================================================

    @Test
    @DisplayName("Un seul compte est créé et aucun champ ne contient le mot de passe en clair")
    void seed_onlyOneAccountCreated_noClearPasswordInAnyField() {
        runStartupSeeder();

        List<GestionAccount> accounts = gestionAccountRepository.findAll();
        assertThat(accounts).hasSize(1);
        GestionAccount admin = accounts.get(0);
        assertThat(admin.getPassword()).doesNotContain(SEED_PASSWORD);
        assertThat(admin.getFullName()).doesNotContain(SEED_PASSWORD);
        assertThat(admin.getEmail()).doesNotContain(SEED_PASSWORD);
        assertThat(admin.getTelephone()).doesNotContain(SEED_PASSWORD);
    }

    @Test
    @DisplayName("Aucun message de log du seeder ne contient le mot de passe en clair")
    void seed_passwordNeverAppearsInLogMessages() {
        Logger seederLogger = (Logger) LoggerFactory.getLogger(AdminSeeder.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        seederLogger.addAppender(appender);

        try {
            runStartupSeeder();
        } finally {
            seederLogger.detachAppender(appender);
            appender.stop();
        }

        assertThat(appender.list).isNotEmpty();
        assertThat(appender.list)
                .allSatisfy(event ->
                        assertThat(event.getFormattedMessage()).doesNotContain(SEED_PASSWORD));
    }
}
