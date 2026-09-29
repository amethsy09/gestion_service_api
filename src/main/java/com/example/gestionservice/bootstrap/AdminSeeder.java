package com.example.gestionservice.bootstrap;

import com.example.gestionservice.entity.GestionAccount;
import com.example.gestionservice.enums.Role;
import com.example.gestionservice.exception.AdminSeedConflictException;
import com.example.gestionservice.repository.GestionAccountRepository;
import com.example.gestionservice.service.NormalisationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Seeder du compte ADMIN initial (Étape 4).
 *
 * <p>Exécuté au démarrage via {@link ApplicationRunner}, une fois le
 * contexte Spring fully initialisé (donc après les migrations Flyway et
 * après la validation du schéma par Hibernate).</p>
 *
 * <p><b>Idempotence</b> — le seeder ne crée un compte que si AUCUN
 * {@code ROLE_ADMIN} n'existe déjà :</p>
 * <pre>
 * admin.seed.enabled = false            → rien (SKIPPED_DISABLED)
 * existsByRole(ROLE_ADMIN) = true       → rien (SKIPPED_ADMIN_ALREADY_EXISTS)
 * sinon                                → création (CREATED)
 * </pre>
 *
 * <p>Lorsqu'un ADMIN existe déjà, le seeder est strictement neutre : il ne
 * lit pas son mot de passe, ne le remplace pas, ne le réactive pas s'il est
 * {@code active = false}, et ne modifie ni son nom, ni son email, ni son
 * téléphone. Plusieurs démarrages successifs (redéploiement, conteneur
 * redémarré) convergent donc toujours vers le même et unique ADMIN.</p>
 *
 * <p>Les informations du compte proviennent exclusivement de la
 * configuration ({@link AdminSeedProperties}) — jamais codées en dur.
 * Le mot de passe en clair n'est jamais journalisé : seuls le téléphone,
 * l'email et l'issue du seeding le sont.</p>
 *
 * <p>En cas d'incohérence (aucun ADMIN mais téléphone ou email déjà pris
 * par un autre compte), le démarrage échoue avec une
 * {@link AdminSeedConflictException} : le compte en conflit n'est ni
 * écrasé, ni modifié.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(AdminSeedProperties.class)
public class AdminSeeder implements ApplicationRunner {

    private final AdminSeedProperties properties;
    private final GestionAccountRepository gestionAccountRepository;
    private final NormalisationService normalisationService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Point d'entrée appelé par Spring au démarrage.
     * Une exception levée ici interrompt le démarrage de l'application.
     */
    @Override
    public void run(ApplicationArguments args) {
        seed();
    }

    /**
     * Issue du seeding, utile pour les tests et les logs.
     */
    public enum SeedResult {
        /** {@code admin.seed.enabled = false} : aucun seeding effectué. */
        SKIPPED_DISABLED,
        /** Un compte {@code ROLE_ADMIN} existait déjà : aucune modification. */
        SKIPPED_ADMIN_ALREADY_EXISTS,
        /** Un compte {@code ROLE_ADMIN} a été créé. */
        CREATED
    }

    /**
     * Garantit qu'un compte ADMIN existe, sans jamais modifier un compte existant.
     *
     * @return l'issue du seeding
     * @throws AdminSeedConflictException si le téléphone ou l'email configuré
     *                                  appartient déjà à un autre compte et
     *                                  qu'aucun ADMIN n'existe
     * @throws IllegalStateException      si la configuration est incomplète
     *                                  (mot de passe ou nom absent)
     */
    public SeedResult seed() {
        // 0. Seeding désactivé → sortie immédiate, sans aucune lecture en base.
        if (!properties.isEnabled()) {
            log.info("Admin seeder désactivé (admin.seed.enabled=false) — aucun compte créé");
            return SeedResult.SKIPPED_DISABLED;
        }

        // 1. Normalisation des identifiants, exactement comme à l'inscription.
        //    Un format invalide doit interrompre le démarrage, pas créer un compte bancal.
        String telephone;
        String email;
        try {
            telephone = normalisationService.normaliseTelephone(properties.getTelephone());
            email = normalisationService.normaliseEmail(properties.getEmail());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Admin seeder : identifiants configurés invalides (admin.seed.telephone / admin.seed.email) : "
                            + e.getMessage(), e);
        }

        // 2. Idempotence : un ADMIN existe déjà → ne rien faire du tout.
        if (gestionAccountRepository.existsByRole(Role.ROLE_ADMIN)) {
            log.info("Admin seeder ignoré : un compte ROLE_ADMIN existe déjà — aucun compte modifié");
            return SeedResult.SKIPPED_ADMIN_ALREADY_EXISTS;
        }

        // 3. Conflits d'unicité : on refuse d'écraser un compte existant.
        //    Les rôles de ces comptes restent inchangés.
        gestionAccountRepository.findByTelephone(telephone).ifPresent(conflicting -> {
            throw new AdminSeedConflictException(
                    "Admin seeder : le téléphone configuré (admin.seed.telephone=" + telephone
                            + ") appartient déjà au compte " + conflicting.getId()
                            + " qui n'est pas un ADMIN. Aucun compte n'a été modifié. "
                            + "Corrigez admin.seed.telephone (ou réattribuez le rôle ROLE_ADMIN à ce compte).");
        });

        gestionAccountRepository.findByEmail(email).ifPresent(conflicting -> {
            throw new AdminSeedConflictException(
                    "Admin seeder : l'email configuré (admin.seed.email=" + email
                            + ") appartient déjà au compte " + conflicting.getId()
                            + " qui n'est pas un ADMIN. Aucun compte n'a été modifié. "
                            + "Corrigez admin.seed.email (ou réattribuez le rôle ROLE_ADMIN à ce compte).");
        });

        // 4. Validation du reste de la configuration.
        //    Le mot de passe vient de la configuration : aucun défaut en Java.
        String rawPassword = properties.getPassword();
        if (!StringUtils.hasText(rawPassword)) {
            throw new IllegalStateException(
                    "Admin seeder : admin.seed.password est vide et aucun compte ROLE_ADMIN n'existe. "
                            + "Définissez la variable d'environnement ADMIN_SEED_PASSWORD pour créer le compte initial.");
        }
        if (!StringUtils.hasText(properties.getFullName())) {
            throw new IllegalStateException(
                    "Admin seeder : admin.seed.full-name est vide et aucun compte ROLE_ADMIN n'existe. "
                            + "Définissez la variable d'environnement ADMIN_SEED_FULL_NAME.");
        }
        String fullName = properties.getFullName().trim();

        // 5. Création : hash BCrypt, rôle forcé à ROLE_ADMIN, actif.
        GestionAccount admin = GestionAccount.builder()
                .fullName(fullName)
                .telephone(telephone)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .role(Role.ROLE_ADMIN)
                .active(true)
                .build();

        try {
            gestionAccountRepository.save(admin);
        } catch (DataIntegrityViolationException e) {
            // Course entre deux instances démarrant simultanément, ou insertion
            // concurrente : les contraintes UNIQUE (telephone, email) sont le filet.
            throw new AdminSeedConflictException(
                    "Admin seeder : impossible de créer le compte ADMIN configuré car son téléphone ou son email "
                            + "est déjà utilisé par un autre compte. Aucun compte n'a été modifié.", e);
        }

        // 6. Log de traçabilité — le mot de passe n'apparaît jamais ici.
        log.info("Admin seeder : compte ROLE_ADMIN créé (telephone={}, email={}, fullName={})",
                telephone, email, fullName);
        return SeedResult.CREATED;
    }
}
