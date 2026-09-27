package com.example.gestionservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Requête de connexion.
 *
 * <p>Le téléphone reste l'identifiant de connexion principal.</p>
 *
 * <p>Exemple :<pre>
 * {
 *   "telephone": "771234567",
 *   "password": "motdepasse123"
 * }
 * </pre></p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    /** Téléphone — obligatoire, normalisé avant recherche. */
    @NotBlank(message = "Le téléphone est obligatoire")
    private String telephone;

    /** Mot de passe — obligatoire, vérifié avec BCrypt. */
    @NotBlank(message = "Le mot de passe est obligatoire")
    private String password;
}