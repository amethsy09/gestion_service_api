package com.example.gestionservice.security;

import com.example.gestionservice.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie le bean {@link PasswordEncoder} préparé pour l'authentification locale.
 *
 * <p>Le bean est créé mais <b>aucun code ne l'utilise encore</b> : ni
 * {@code register} ni {@code login} ne sont implémentés à cette étape.
 * Ce test garantit qu'il est nonetheless disponible et correctement réglé
 * pour l'étape 3.</p>
 */
@DisplayName("SecurityConfig — PasswordEncoder BCrypt")
class PasswordEncoderIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Le bean PasswordEncoder est disponible dans le contexte")
    void passwordEncoder_beanIsPresent() {
        assertThat(passwordEncoder).isNotNull();
    }

    @Test
    @DisplayName("Le hachage utilise BCrypt avec un coût de 12")
    void passwordEncoder_usesBcryptWithCost12() {
        String encoded = passwordEncoder.encode("mot-de-passe-test");

        // Format BCrypt : $2a$<coût>$<sel><hash>
        assertThat(encoded).startsWith("$2a$12$");
        assertThat(encoded).hasSize(60);
    }

    @Test
    @DisplayName("Le mot de passe en clair n'apparaît jamais dans le hash")
    void passwordEncoder_neverStoresPlaintext() {
        String plain = "MotDePasseEnClair123";

        String encoded = passwordEncoder.encode(plain);

        assertThat(encoded).doesNotContain(plain);
    }

    @Test
    @DisplayName("matches valide le bon mot de passe et rejette les autres")
    void passwordEncoder_matchesVerifiesCorrectPassword() {
        String encoded = passwordEncoder.encode("MotDePasseEnClair123");

        assertThat(passwordEncoder.matches("MotDePasseEnClair123", encoded)).isTrue();
        assertThat(passwordEncoder.matches("mauvais-mot-de-passe", encoded)).isFalse();
    }

    @Test
    @DisplayName("Deux hachages du même mot de passe diffèrent (sel aléatoire)")
    void passwordEncoder_usesRandomSalt() {
        String first = passwordEncoder.encode("MotDePasseEnClair123");
        String second = passwordEncoder.encode("MotDePasseEnClair123");

        assertThat(first).isNotEqualTo(second);
        assertThat(passwordEncoder.matches("MotDePasseEnClair123", first)).isTrue();
        assertThat(passwordEncoder.matches("MotDePasseEnClair123", second)).isTrue();
    }

    @Test
    @DisplayName("Un hash corrompu en base n'authentifie personne et ne fait pas échouer la connexion")
    void passwordEncoder_malformedHash_isRejectedWithoutThrowing() {
        // Un hash altéré en base ne doit jamais valider un mot de passe, et ne
        // doit pas faire planter la connexion : matches renvoie false.
        assertThat(passwordEncoder.matches("mot-de-passe", "pas-un-hash")).isFalse();
        assertThat(passwordEncoder.matches("mot-de-passe", "")).isFalse();
        assertThat(passwordEncoder.matches("mot-de-passe", null)).isFalse();
    }
}
