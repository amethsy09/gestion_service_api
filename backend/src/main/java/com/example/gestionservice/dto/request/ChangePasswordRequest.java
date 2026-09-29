package com.example.gestionservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Requête de changement de mot de passe.
 *
 * <p>Utilisée par l'utilisateur connecté pour définir un nouveau mot de passe
 * (remplacement du mot de passe temporaire).</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {

    /** Ancien mot de passe (obligatoire pour vérifier l'identité). */
    @NotBlank(message = "L'ancien mot de passe est obligatoire")
    private String currentPassword;

    /** Nouveau mot de passe — 8 à 72 caractères. */
    @NotBlank(message = "Le nouveau mot de passe est obligatoire")
    @Size(min = 8, max = 72, message = "Le mot de passe doit contenir entre 8 et 72 caractères")
    private String newPassword;
}