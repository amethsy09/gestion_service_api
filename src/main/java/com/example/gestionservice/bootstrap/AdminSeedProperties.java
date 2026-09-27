package com.example.gestionservice.bootstrap;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration du seeding du compte ADMIN initial (Étape 4).
 *
 * <p>Préfixe : {@code admin.seed}. Exemple :</p>
 * <pre>
 * admin:
 *   seed:
 *     enabled: ${ADMIN_SEED_ENABLED:true}
 *     full-name: ${ADMIN_SEED_FULL_NAME:Administrator}
 *     telephone: ${ADMIN_SEED_TELEPHONE:771234567}
 *     email: ${ADMIN_SEED_EMAIL:admin@example.com}
 *     password: ${ADMIN_SEED_PASSWORD:}
 * </pre>
 *
 * <p>Le mot de passe n'a AUCUNE valeur par défaut : il doit venir de
 * l'environnement ou d'un fichier de secrets. Il n'est jamais codé en dur
 * dans le Java, et il est exclu de {@code toString()} pour qu'un log
 * accidentel du bean ne puisse pas le révéler.</p>
 *
 * <p>Si {@code admin.seed.password} est vide alors qu'aucun ADMIN n'existe,
 * le démarrage échoue explicitement plutôt que de créer un compte avec un
 * mot de passe devinable ou vide.</p>
 */
@Getter
@Setter
@ToString(exclude = "password")
@ConfigurationProperties(prefix = "admin.seed")
public class AdminSeedProperties {

    /**
     * Active ou non le seeder au démarrage.
     * {@code false} = aucun seeding, même si aucun ADMIN n'existe.
     */
    private boolean enabled = true;

    /**
     * Nom complet du compte ADMIN initial.
     */
    private String fullName;

    /**
     * Téléphone du compte ADMIN initial.
     * Normalisé par {@link com.example.gestionservice.service.NormalisationService}
     * avant vérification et stockage.
     */
    private String telephone;

    /**
     * Email du compte ADMIN initial.
     * Normalisé (minuscules) avant vérification et stockage.
     */
    private String email;

    /**
     * Mot de passe EN CLAIR du compte ADMIN initial, fourni par la configuration.
     * Haché avec BCrypt avant stockage — jamais persisté ni journalisé tel quel.
     */
    private String password;
}
