package com.example.gestionservice.dto.response;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Réponse d'authentification (inscription ou connexion).
 *
 * <p>Aucune donnée sensible n'est exposée : ni mot de passe, ni hash,
 * ni email, ni téléphone, ni UUID du compte (sauf dans le JWT).</p>
 *
 * <p>Format minimum :<pre>
 * {
 *   "token": "...",
 *   "role": "ROLE_USER"
 * }
 * </pre></p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /** Token JWT local (sub = UUID du compte). */
    private String token;

    /** Rôle de l'utilisateur (ex: ROLE_USER). */
    private String role;
}