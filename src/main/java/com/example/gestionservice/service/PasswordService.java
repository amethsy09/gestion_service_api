package com.example.gestionservice.service;

import com.example.gestionservice.entity.GestionAccount;
import com.example.gestionservice.exception.BusinessException;
import com.example.gestionservice.exception.UnauthorizedException;
import com.example.gestionservice.exception.ValidationException;
import com.example.gestionservice.repository.GestionAccountRepository;
import com.example.gestionservice.service.NormalisationService;
import com.example.gestionservice.service.TemporaryPasswordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service de gestion du mot de passe et du premier changement.
 *
 * <p>Gère les opérations suivantes :
 * <ul>
 *   <li>Changement de mot de passe par l'utilisateur connecté</li>
 *   <li>Vérification du flag {@code mustChangePassword}</li>
 * </ul></p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordService {

    private final GestionAccountRepository gestionAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final TemporaryPasswordService temporaryPasswordService;

    /**
     * Change le mot de passe d'un compte.
     *
     * @param accountId    UUID du compte
     * @param currentPassword ancien mot de passe (doit être correct)
     * @param newPassword  nouveau mot de passe (8 à 72 caractères)
     * @throws UnauthorizedException si l'ancien mot de passe est incorrect
     * @throws ValidationException si le nouveau mot de passe ne respecte pas les règles
     */
    @Transactional
    public void changePassword(UUID accountId, String currentPassword, String newPassword) {
        GestionAccount account = gestionAccountRepository.findById(accountId)
                .orElseThrow(() -> new UnauthorizedException("Compte introuvable"));

        // Vérifier l'ancien mot de passe
        if (!passwordEncoder.matches(currentPassword, account.getPassword())) {
            throw new UnauthorizedException("L'ancien mot de passe est incorrect");
        }

        // Valider le nouveau mot de passe
        if (!temporaryPasswordService.isValid(newPassword)) {
            throw new ValidationException(
                    "Le mot de passe doit contenir au moins 8 caractères, " +
                    "dont une lettre majuscule, une lettre minuscule et un chiffre");
        }

        // Hasher et sauvegarder
        String hashedPassword = passwordEncoder.encode(newPassword);
        account.setPassword(hashedPassword);
        account.setMustChangePassword(false);
        gestionAccountRepository.save(account);

        log.info("Mot de passe changé pour le compte id={}", accountId);
    }
}