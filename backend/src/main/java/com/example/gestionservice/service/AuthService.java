package com.example.gestionservice.service;

import com.example.gestionservice.dto.request.LoginRequest;
import com.example.gestionservice.dto.request.RegisterRequest;
import com.example.gestionservice.dto.response.AuthResponse;
import com.example.gestionservice.entity.GestionAccount;
import com.example.gestionservice.enums.Role;
import com.example.gestionservice.exception.ConflictException;
import com.example.gestionservice.exception.UnauthorizedException;
import com.example.gestionservice.exception.ValidationException;
import com.example.gestionservice.repository.GestionAccountRepository;
import com.example.gestionservice.security.JwtService;
import com.example.gestionservice.service.NormalisationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service d'authentification locale : inscription et connexion.
 *
 * <p>Flux d'inscription :<pre>
 * RegisterRequest
 *   → validation (Bean Validation)
 *   → normalisation téléphone + email
 *   → vérification unicité (téléphone, email)
 *   → hash BCrypt du mot de passe
 *   → création GestionAccount (role = ROLE_USER, active = true)
 *   → save
 *   → génération JWT local (sub = UUID)
 *   → AuthResponse</pre>
 *
 * <p>Flux de connexion :<pre>
 * LoginRequest
 *   → normalisation téléphone
 *   → recherche compte
 *   → vérification active = true
 *   → vérification mot de passe BCrypt
 *   → génération JWT local (sub = UUID)
 *   → AuthResponse</pre>
 *
 * <p>Si le compte a {@code mustChangePassword = true}, la connexion est refusée
 * jusqu'à ce que l'utilisateur ait changé son mot de passe via
 * {@code PUT /api/v1/auth/change-password}.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final GestionAccountRepository gestionAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final NormalisationService normalisationService;

    // -----------------------------------------------------------------
    //  Inscription
    // -----------------------------------------------------------------

    /**
     * Inscription d'un nouvel utilisateur.
     *
     * @param request les données d'inscription (déjà validées par Bean Validation)
     * @return AuthResponse contenant le JWT et le rôle
     * @throws ValidationException  si le format du téléphone ou de l'email est invalide
     * @throws ConflictException     si le téléphone ou l'est déjà utilisé
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 1. Normaliser le téléphone et l'email avant toute recherche/stockage.
        String telephone;
        String email;
        try {
            telephone = normalisationService.normaliseTelephone(request.getTelephone());
            email = normalisationService.normaliseEmail(request.getEmail());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Format invalide : " + e.getMessage(), e);
        }
        String fullName = request.getFullName().trim();

        // 2. Vérifier l'unicité (pré-check en code, la base garantit aussi via contraintes).
        if (gestionAccountRepository.existsByTelephone(telephone)) {
            throw new ConflictException("Ce numéro de téléphone est déjà utilisé");
        }
        if (gestionAccountRepository.existsByEmail(email)) {
            throw new ConflictException("Cette adresse email est déjà utilisée");
        }

        // 3. Hash BCrypt du mot de passe.
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // 4. Création du compte : rôle forcé à ROLE_USER, active = true.
        GestionAccount account = GestionAccount.builder()
                .fullName(fullName)
                .telephone(telephone)
                .email(email)
                .password(hashedPassword)
                .role(Role.ROLE_USER)
                .active(true)
                .mustChangePassword(false)
                .build();

        try {
            account = gestionAccountRepository.save(account);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Une contrainte PostgreSQL a été violée (concurrence).
            // On détermine quelle contrainte a échoué pour un message clair.
            String message = "Inscription impossible : identifiant déjà utilisé";
            String lower = e.getMessage();
            if (lower.contains("telephone") || lower.contains("uk_gestion_account_telephone")) {
                message = "Ce numéro de téléphone est déjà utilisé";
            } else if (lower.contains("email") || lower.contains("uk_gestion_account_email")) {
                message = "Cette adresse email est déjà utilisée";
            }
            throw new ConflictException(message, e);
        }

        // 5. Générer le JWT local (sub = UUID du compte).
        String token = jwtService.generateToken(account.getId());

        log.info("Inscription réussie pour telephone={} email={}", telephone, email);

        return AuthResponse.builder()
                .token(token)
                .role(account.getRole().name())
                .build();
    }

    // -----------------------------------------------------------------
    //  Connexion
    // -----------------------------------------------------------------

    /**
     * Connexion d'un utilisateur existant.
     *
     * @param request les données de connexion (déjà validées par Bean Validation)
     * @return AuthResponse contenant le JWT et le rôle
     * @throws UnauthorizedException si le compte est introuvable, désactivé,
     *                                si le mot de passe est incorrect, ou si
     *                                le compte nécessite un changement de mot de passe
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // 1. Normaliser le téléphone.
        String telephone;
        try {
            telephone = normalisationService.normaliseTelephone(request.getTelephone());
        } catch (IllegalArgumentException e) {
            throw new UnauthorizedException("Identifiants invalides");
        }

        // 2. Rechercher le compte.
        GestionAccount account = gestionAccountRepository.findByTelephone(telephone)
                .orElseThrow(() -> new UnauthorizedException("Identifiants invalides"));

        // 3. Vérifier active = true.
        if (!account.getActive()) {
            throw new UnauthorizedException("Compte désactivé");
        }

        // 4. Vérifier le mot de passe avec BCrypt.
        if (!passwordEncoder.matches(request.getPassword(), account.getPassword())) {
            throw new UnauthorizedException("Identifiants invalides");
        }

        // 5. Vérifier si le compte nécessite un changement de mot de passe.
        // Le compte est créé avec un mot de passe temporaire (par un admin).
        // L'utilisateur doit d'abord changer le mot de passe avant de pouvoir accéder à l'application.
        if (Boolean.TRUE.equals(account.getMustChangePassword())) {
            throw new UnauthorizedException(
                    "Vous devez changer votre mot de passe temporaire avant de pouvoir vous connecter. " +
                    "Utilisez l'endpoint POST /api/v1/auth/first-change-password avec votre téléphone, " +
                    "votre mot de passe temporaire et votre nouveau mot de passe.");
        }

        // 6. Générer le JWT local (sub = UUID du compte).
        String token = jwtService.generateToken(account.getId());

        log.info("Connexion réussie pour telephone={}", telephone);

        return AuthResponse.builder()
                .token(token)
                .role(account.getRole().name())
                .build();
    }
}