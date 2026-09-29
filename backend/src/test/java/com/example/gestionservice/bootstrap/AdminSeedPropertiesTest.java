package com.example.gestionservice.bootstrap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de liaison de configuration du seeder ADMIN (Étape 4).
 *
 * <p>Vérifie que le préfixe {@code admin.seed} est bien relié au bean
 * {@link AdminSeedProperties}, que le style kebab-case ({@code full-name})
 * fonctionne comme dans {@code application.yml}, et que le mot de passe
 * n'apparaît jamais dans {@code toString()}.</p>
 */
@DisplayName("AdminSeedProperties — Tests de configuration")
class AdminSeedPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Configuration
    @EnableConfigurationProperties(AdminSeedProperties.class)
    static class TestConfig {
    }

    // ==================================================================
    //  Liaison des propriétés
    // ==================================================================

    @Test
    @DisplayName("Propriétés admin.seed.* reliées au bean")
    void properties_boundToBean() {
        contextRunner
                .withPropertyValues(
                        "admin.seed.enabled=true",
                        "admin.seed.full-name=Administrateur",
                        "admin.seed.telephone=771234567",
                        "admin.seed.email=admin@example.com",
                        "admin.seed.password=un-mot-de-passe-solide")
                .run(context -> {
                    AdminSeedProperties properties = context.getBean(AdminSeedProperties.class);
                    assertThat(properties.isEnabled()).isTrue();
                    assertThat(properties.getFullName()).isEqualTo("Administrateur");
                    assertThat(properties.getTelephone()).isEqualTo("771234567");
                    assertThat(properties.getEmail()).isEqualTo("admin@example.com");
                    assertThat(properties.getPassword()).isEqualTo("un-mot-de-passe-solide");
                });
    }

    @Test
    @DisplayName("Nom de propriété en kebab-case (full-name) → fullName")
    void kebabCaseKey_bindsToFullName() {
        contextRunner
                .withPropertyValues(
                        "admin.seed.enabled=true",
                        "admin.seed.full-name=Administrateur seed",
                        "admin.seed.password=un-mot-de-passe-solide")
                .run(context -> assertThat(context.getBean(AdminSeedProperties.class).getFullName())
                        .isEqualTo("Administrateur seed"));
    }

    @Test
    @DisplayName("Sans configuration : enabled = true, aucun mot de passe par défaut")
    void defaults_noHardcodedPassword() {
        contextRunner.run(context -> {
            AdminSeedProperties properties = context.getBean(AdminSeedProperties.class);
            assertThat(properties.isEnabled()).isTrue();
            // Aucune valeur par défaut en dur : c'est le seeder qui refuse
            // de démarrer si le mot de passe est vide.
            assertThat(properties.getPassword()).isNull();
            assertThat(properties.getTelephone()).isNull();
            assertThat(properties.getEmail()).isNull();
        });
    }

    @Test
    @DisplayName("Mot de passe exclu de toString() — jamais exposé par un log accidentel")
    void password_excludedFromToString() {
        contextRunner
                .withPropertyValues(
                        "admin.seed.enabled=true",
                        "admin.seed.full-name=Administrateur",
                        "admin.seed.telephone=771234567",
                        "admin.seed.email=admin@example.com",
                        "admin.seed.password=secret-ne-doit-pas-apparaitre")
                .run(context -> {
                    String rendered = context.getBean(AdminSeedProperties.class).toString();
                    assertThat(rendered).doesNotContain("secret-ne-doit-pas-apparaitre");
                    assertThat(rendered).doesNotContain("password=");
                    assertThat(rendered).doesNotContain("getPassword");
                });
    }
}
