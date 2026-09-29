package com.example.gestionservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Requête de premier changement de mot de passe (sans JWT).
 *
 * <p>Utilisée par l'utilisateur qui a reçu un mot de passe temporaire par email.
 * L'accès est public : on identifie l'utilisateur par son téléphone et l'ancien mot de passe.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FirstChangePasswordRequest {

    /** Téléphone — identifiant de connexion. */
    @NotBlank(message = "Le téléphone est obligatoire")
    private String telephone;

    /** Mot de passe temporaire (envoyé par email). */
    @NotBlank(message = "L'ancien mot de passe est obligatoire")
    private String temporaryPassword;

    /** Nouveau mot de passe — 8 à 72 caractères. */
    @NotBlank(message = "Le nouveau mot de passe est obligatoire")
    @Size(min = 8, max = 72, message = "Le mot de passe doit contenir entre 8 et 72 caractères")
    private String newPassword;
}