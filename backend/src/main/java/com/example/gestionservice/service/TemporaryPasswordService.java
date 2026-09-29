package com.example.gestionservice.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;

/**
 * Service de génération et de validation des mots de passe temporaires.
 *
 * <p>Un mot de passe temporaire est une chaîne aléatoire facilement
 * lisible par l'utilisateur (lettres majuscules, minuscules, chiffres),
 * mais suffisamment complexe pour respecter les règles de sécurité.</p>
 */
@Service
public class TemporaryPasswordService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** Longueur du mot de passe temporaire. */
    private static final int TEMP_PASSWORD_LENGTH = 12;

    /** Caractères acceptés pour le mot de passe temporaire. */
    private static final String ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    /**
     * Génère un mot de passe temporaire aléatoire.
     *
     * @return une chaîne de 12 caractères aléatoires (majuscules, minuscules, chiffres)
     */
    public String generateTemporaryPassword() {
        StringBuilder sb = new StringBuilder(TEMP_PASSWORD_LENGTH);
        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            sb.append(ALPHABET.charAt(SECURE_RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    /**
     * Vérifie si un mot de passe respecte les règles minimales.
     *
     * @param password mot de passe à vérifier
     * @return true si le mot de passe contient au moins une majuscule,
     *         une minuscule et un chiffre
     */
    public boolean isValid(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            if (Character.isLowerCase(c)) hasLower = true;
            if (Character.isDigit(c)) hasDigit = true;
        }
        return hasUpper && hasLower && hasDigit;
    }
}