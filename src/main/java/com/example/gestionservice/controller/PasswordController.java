package com.example.gestionservice.controller;

import com.example.gestionservice.dto.request.ChangePasswordRequest;
import com.example.gestionservice.dto.response.ApiResponse;
import com.example.gestionservice.security.JwtAuthenticationPrincipal;
import com.example.gestionservice.service.PasswordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur pour la gestion des mots de passe.
 *
 * <p>Protégé par JWT : l'utilisateur doit être authentifié.</p>
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentification locale (inscription / connexion)")
@SecurityRequirement(name = "bearerAuth")
public class PasswordController {

    private final PasswordService passwordService;

    /**
     * Change le mot de passe de l'utilisateur connecté.
     *
     * <p>Permet de remplacer le mot de passe temporaire par un mot de passe définitif.</p>
     *
     * @param request contenant l'ancien et le nouveau mot de passe
     * @return 200 OK si le changement a réussi
     */
    @PutMapping("/change-password")
    @Operation(summary = "Changer le mot de passe")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal) {
        passwordService.changePassword(principal.getAccountId(), request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("Mot de passe changé avec succès", null));
    }
}