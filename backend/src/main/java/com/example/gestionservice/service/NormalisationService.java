package com.example.gestionservice.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Service de normalisation des identifiants (téléphone, email).
 *
 * <p>La normalisation est appliquée avant toute recherche ou stockage,
 * afin de garantir l'unicité indépendamment de la façon dont le client
 * saisit la valeur.</p>
 */
@Service
public class NormalisationService {

    // -----------------------------------------------------------------
    //  Téléphone sénégalais
    // -----------------------------------------------------------------

    /**
     * Normalise un numéro de téléphone sénégalais vers sa forme canonique.
     *
     * <p>Formats acceptés (tous aboutissant à {@code 771234567}) :</p>
     * <ul>
     *   <li>{@code +221 77 123 45 67}  → 771234567</li>
     *   <li>{@code 00221 77 123 45 67} → 771234567</li>
     *   <li>{@code 771234567}           → 771234567</li>
     * </ul>
     *
     * @param telephone numéro tel quel saisi par le client
     * @return numéro canonique (9 chiffres, commence par 7)
     * @throws IllegalArgumentException si le numéro est vide ou non valide
     */
    public String normaliseTelephone(String telephone) {
        if (telephone == null) {
            throw new IllegalArgumentException("Le téléphone ne peut pas être nul");
        }
        if (!StringUtils.hasText(telephone)) {
            throw new IllegalArgumentException("Le téléphone est obligatoire");
        }

        // 1. Garder uniquement les chiffres.
        String digits = telephone.replaceAll("[^0-9]", "");

        // 2. Supprimer le préfixe international s'il est présent.
        if (digits.startsWith("00221")) {
            digits = digits.substring(5);
        } else if (digits.startsWith("221") && digits.length() == 12) {
            digits = digits.substring(3);
        }

        // 3. Valider : 9 chiffres, commençant par 7.
        if (digits.length() != 9) {
            throw new IllegalArgumentException(
                    "Numéro de téléphone invalide : " + telephone + " (attendu : 9 chiffres)");
        }
        if (!digits.startsWith("7")) {
            throw new IllegalArgumentException(
                    "Numéro de téléphone invalide : " + telephone + " (doit commencer par 7)");
        }

        return digits;
    }

    // -----------------------------------------------------------------
    //  Email
    // -----------------------------------------------------------------

    /**
     * Normalise une adresse email :
     * <ul>
     *   <li>Supprime les espaces inutiles (autour et à l'intérieur)</li>
     *   <li>Convertit en minuscules</li>
     * </ul>
     *
     * @param email email tel quel saisi par le client
     * @return email normalisé
     * @throws IllegalArgumentException si l'email est vide
     */
    public String normaliseEmail(String email) {
        if (email == null) {
            throw new IllegalArgumentException("L'email ne peut pas être nul");
        }
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("L'email est obligatoire");
        }

        // Supprimer les espaces autour et à l'intérieur (entre les mots).
        String normalised = email.trim().replaceAll("\\s+", "");

        // Convertir en minuscules (les emails sont case-insensitive).
        return normalised.toLowerCase();
    }
}