package com.example.gestionservice.service;

import com.example.gestionservice.dto.request.FirstChangePasswordRequest;
import com.example.gestionservice.entity.GestionAccount;
import com.example.gestionservice.exception.UnauthorizedException;
import com.example.gestionservice.exception.ValidationException;
import com.example.gestionservice.repository.GestionAccountRepository;
import com.example.gestionservice.service.NormalisationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service de premier changement de mot de passe (sans authentification JWT).
 *
 * <p>Permet à un utilisateur qui a reçu un mot de passe temporaire par email
 * de le remplacer par un mot de passe définitif, sans avoir à se connecter
 * au préalable (le login refuse les comptes avec mot de passe temporaire).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FirstChangePasswordService {

    private final GestionAccountRepository gestionAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final TemporaryPasswordService temporaryPasswordService;
    private final NormalisationService normalisationService;

    /**
     * Change le mot de passe temporaire d'un compte (première connexion).
     *
     * @param request contenant le téléphone, le mot de passe temporaire et le nouveau mot de passe
     * @throws UnauthorizedException si le compte est introuvable, le mot de passe temporaire est incorrect,
     *                                ou le compte n'a pas de mot de passe temporaire
     * @throws ValidationException si le nouveau mot de passe ne respecte pas les règles
     */
    @Transactional
    public void firstChangePassword(FirstChangePasswordRequest request) {
        // 1. Normaliser le téléphone
        String telephone;
        try {
            telephone = normalisationService.normaliseTelephone(request.getTelephone());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Format de téléphone invalide : " + e.getMessage(), e);
        }

        // 2. Rechercher le compte
        GestionAccount account = gestionAccountRepository.findByTelephone(telephone)
                .orElseThrow(() -> new UnauthorizedException("Compte introuvable"));

        // 3. Vérifier que le compte a bien un mot de passe temporaire
        if (Boolean.FALSE.equals(account.getMustChangePassword())) {
            throw new UnauthorizedException(
                    "Ce compte n'a pas de mot de passe temporaire. Utilisez l'endpoint de changement de mot de passe standard.");
        }

        // 4. Vérifier le mot de passe temporaire
        if (!passwordEncoder.matches(request.getTemporaryPassword(), account.getPassword())) {
            throw new UnauthorizedException("Le mot de passe temporaire est incorrect");
        }

        // 5. Valider le nouveau mot de passe
        if (!temporaryPasswordService.isValid(request.getNewPassword())) {
            throw new ValidationException(
                    "Le mot de passe doit contenir au moins 8 caractères, " +
                    "dont une lettre majuscule, une lettre minuscule et un chiffre");
        }

        // 6. Hasher et sauvegarder le nouveau mot de passe
        String hashedPassword = passwordEncoder.encode(request.getNewPassword());
        account.setPassword(hashedPassword);
        account.setMustChangePassword(false);
        gestionAccountRepository.save(account);

        log.info("Premier changement de mot de passe réussit pour le compte id={}", account.getId());
    }
}