package com.example.gestionservice.controller;

import com.example.gestionservice.dto.request.FirstChangePasswordRequest;
import com.example.gestionservice.dto.response.ApiResponse;
import com.example.gestionservice.service.FirstChangePasswordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur pour le premier changement de mot de passe (sans authentification).
 *
 * <p>Endpoint public : accessible sans JWT, car l'utilisateur n'a pas encore
 * de mot de passe définitif et ne peut donc pas se connecter.</p>
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentification locale (inscription / connexion)")
public class FirstChangePasswordController {

    private final FirstChangePasswordService firstChangePasswordService;

    /**
     * Change le mot de passe temporaire (première connexion).
     *
     * @param request contenant le téléphone, le mot de passe temporaire et le nouveau mot de passe
     * @return 200 OK si le changement a réussi
     */
    @PostMapping("/first-change-password")
    @Operation(summary = "Premier changement de mot de passe")
    public ResponseEntity<ApiResponse<Void>> firstChangePassword(
            @Valid @RequestBody FirstChangePasswordRequest request) {
        firstChangePasswordService.firstChangePassword(request);
        return ResponseEntity.ok(ApiResponse.success(
                "Mot de passe temporaire remplacé avec succès. Vous pouvez maintenant vous connecter.", null));
    }
}