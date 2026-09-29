package com.example.gestionservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitaires du {@code NormalisationService}.
 *
 * <p>Vérifie que tous les formats de saisie d'un téléphone sénégalais
 * aboutissent au même numéro canonique, et que l'email est nettoyé
 * (minuscules, espaces supprimés) avant stockage.</p>
 */
@DisplayName("NormalisationService — Tests unitaires")
class NormalisationServiceTest {

    private static final String CANONICAL = "771234567";

    private NormalisationService normalisationService;

    @BeforeEach
    void setUp() {
        normalisationService = new NormalisationService();
    }

    // ==================================================================
    //  Téléphone — formats équivalents
    // ==================================================================

    @Test
    @DisplayName("Tous les formats aboutissent au même numéro canonique")
    void telephone_allFormatsProduceSameCanonical() {
        String a = normalisationService.normaliseTelephone("+221 77 123 45 67");
        String b = normalisationService.normaliseTelephone("00221 77 123 45 67");
        String c = normalisationService.normaliseTelephone("771234567");

        assertThat(a).isEqualTo(CANONICAL);
        assertThat(b).isEqualTo(CANONICAL);
        assertThat(c).isEqualTo(CANONICAL);
    }

    @Test
    @DisplayName("+221 espaces → canonique")
    void telephone_plus221WithSpaces_normalized() {
        assertThat(normalisationService.normaliseTelephone("+221 77 123 45 67"))
                .isEqualTo(CANONICAL);
    }

    @Test
    @DisplayName("+221 sans espaces → canonique")
    void telephone_plus221NoSpaces_normalized() {
        assertThat(normalisationService.normaliseTelephone("+221771234567"))
                .isEqualTo(CANONICAL);
    }

    @Test
    @DisplayName("00221 espaces → canonique")
    void telephone_00221WithSpaces_normalized() {
        assertThat(normalisationService.normaliseTelephone("00221 77 123 45 67"))
                .isEqualTo(CANONICAL);
    }

    @Test
    @DisplayName("00221 sans espaces → canonique")
    void telephone_00221NoSpaces_normalized() {
        assertThat(normalisationService.normaliseTelephone("00221771234567"))
                .isEqualTo(CANONICAL);
    }

    @Test
    @DisplayName("Numéro canonique fourni tel quel")
    void telephone_canonicalReturnedAsIs() {
        assertThat(normalisationService.normaliseTelephone("771234567"))
                .isEqualTo(CANONICAL);
    }

    @Test
    @DisplayName("Espaces autour du numéro → canonique")
    void telephone_withSurroundingSpaces_normalized() {
        assertThat(normalisationService.normaliseTelephone("  771234567  "))
                .isEqualTo(CANONICAL);
    }

    @Test
    @DisplayName("Tirets et points → canonique")
    void telephone_withSeparators_normalized() {
        assertThat(normalisationService.normaliseTelephone("+221-77.123.45.67"))
                .isEqualTo(CANONICAL);
    }

    // ==================================================================
    //  Téléphone — valeurs rejetées
    // ==================================================================

    @Test
    @DisplayName("Téléphone null → IllegalArgumentException")
    void telephone_null_rejected() {
        assertThatThrownBy(() -> normalisationService.normaliseTelephone(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Téléphone vide → IllegalArgumentException")
    void telephone_empty_rejected() {
        assertThatThrownBy(() -> normalisationService.normaliseTelephone(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Téléphone blanc → IllegalArgumentException")
    void telephone_blank_rejected() {
        assertThatThrownBy(() -> normalisationService.normaliseTelephone("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Téléphone trop court (8 chiffres) → IllegalArgumentException")
    void telephone_tooShort_rejected() {
        assertThatThrownBy(() -> normalisationService.normaliseTelephone("77123456"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Téléphone ne commençant pas par 7 → IllegalArgumentException")
    void telephone_notStartingWith7_rejected() {
        assertThatThrownBy(() -> normalisationService.normaliseTelephone("301234567"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("commencer par 7");
    }

    @Test
    @DisplayName("Téléphone alphabétique → IllegalArgumentException")
    void telephone_alpha_rejected() {
        assertThatThrownBy(() -> normalisationService.normaliseTelephone("abcdefghijklmnop"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ==================================================================
    //  Email — normalisation
    // ==================================================================

    @Test
    @DisplayName("Email en majuscules → minuscules")
    void email_uppercaseNormalizedToLowercase() {
        assertThat(normalisationService.normaliseEmail("ALASSANE@EXAMPLE.COM"))
                .isEqualTo("alassane@example.com");
    }

    @Test
    @DisplayName("Email avec majuscules et minuscules → minuscules")
    void email_mixedCaseNormalizedToLowercase() {
        assertThat(normalisationService.normaliseEmail("Test.User@Example.COM"))
                .isEqualTo("test.user@example.com");
    }

    @Test
    @DisplayName("Email avec espaces superflus → espaces supprimés + minuscules")
    void email_spacesRemovedAndLowercased() {
        assertThat(normalisationService.normaliseEmail("  Test @ Example.COM  "))
                .isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Email interne avec espace → espace supprimé")
    void email_internalSpacesRemoved() {
        assertThat(normalisationService.normaliseEmail("test @ example.com"))
                .isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Email déjà normalisé → identique")
    void email_alreadyNormalized_unchanged() {
        assertThat(normalisationService.normaliseEmail("test@example.com"))
                .isEqualTo("test@example.com");
    }

    // ==================================================================
    //  Email — valeurs rejetées
    // ==================================================================

    @Test
    @DisplayName("Email null → IllegalArgumentException")
    void email_null_rejected() {
        assertThatThrownBy(() -> normalisationService.normaliseEmail(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Email vide → IllegalArgumentException")
    void email_empty_rejected() {
        assertThatThrownBy(() -> normalisationService.normaliseEmail(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Email blanc → IllegalArgumentException")
    void email_blank_rejected() {
        assertThatThrownBy(() -> normalisationService.normaliseEmail("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
