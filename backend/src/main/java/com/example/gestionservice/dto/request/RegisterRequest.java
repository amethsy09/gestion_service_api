package com.example.gestionservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Requête d'inscription publique.
 *
 * <p>Seuls les quatre champs suivants sont acceptés du client.</p>
 *
 * <p>Les champs {@code role}, {@code active}, {@code id} et
 * {@code walletAccountId} ne peuvent PAS être envoyés par le client :
 * ils sont systématiquement surchargés côté serveur.</p>
 *
 * <p>Exemple :<pre>
 * {
 *   "fullName": "Alassane Diallo",
 *   "telephone": "771234567",
 *   "email": "alassane@example.com",
 *   "password": "motdepasse123"
 * }
 * </pre></p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    /** Nom complet — obligatoire, non vide (uniquement des espaces rejeté). */
    @NotBlank(message = "Le nom complet est obligatoire")
    private String fullName;

    /** Téléphone — obligatoire, normalisé avant stockage. */
    @NotBlank(message = "Le téléphone est obligatoire")
    private String telephone;

    /** Email — obligatoire, normalisé (minuscules, sans espaces). */
    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;

    /** Mot de passe — obligatoire, 8 à 72 caractères. */
    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, max = 72, message = "Le mot de passe doit contenir entre 8 et 72 caractères")
    private String password;
}