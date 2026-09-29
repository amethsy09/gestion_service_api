package com.example.gestionservice.service;

import com.example.gestionservice.dto.request.ResponsibleRequest;
import com.example.gestionservice.dto.response.ResponsibleResponse;
import com.example.gestionservice.entity.GestionAccount;
import com.example.gestionservice.entity.Responsible;
import com.example.gestionservice.enums.Role;
import com.example.gestionservice.exception.BusinessException;
import com.example.gestionservice.mapper.ResponsibleMapper;
import com.example.gestionservice.repository.GestionAccountRepository;
import com.example.gestionservice.repository.ResponsibleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service de création d'un responsable avec compte d'authentification.
 *
 * <p>Lorsqu'un admin crée un responsable :
 * <ol>
 *   <li>Un compte {@code GestionAccount} est créé avec un mot de passe temporaire.</li>
 *   <li>Le champ {@code mustChangePassword} est positionné à {@code true}.</li>
 *   <li>Un email est envoyé via Gmail SMTP avec le mot de passe temporaire.</li>
 * </ol>
 * L'envoi d'email est best-effort : un échec (identifiants invalides ou
 * service SMTP indisponible) est loggé mais n'empêche pas la création
 * du compte.
 * <p>Le responsable devra changer son mot de passe à la première connexion.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResponsibleAccountService {

    private final ResponsibleRepository responsibleRepository;
    private final GestionAccountRepository gestionAccountRepository;
    private final ResponsibleMapper responsibleMapper;
    private final PasswordEncoder passwordEncoder;
    private final TemporaryPasswordService temporaryPasswordService;
    private final EmailService emailService;
    private final NormalisationService normalisationService;

    /**
     * Crée un responsable et son compte d'authentification avec mot de passe temporaire.
     *
     * @param request données du responsable
     * @return la réponse du responsable créé
     * @throws BusinessException si un compte avec le même email ou téléphone existe déjà
     */
    @Transactional
    public ResponsibleResponse createWithTemporaryPassword(ResponsibleRequest request) {
        // 1. Créer le responsable
        if (responsibleRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new BusinessException("Un responsable avec l'email '" + request.getEmail() + "' existe déjà");
        }

        Responsible entity = responsibleMapper.toEntity(request);
        entity = responsibleRepository.save(entity);
        log.info("Responsable créé : id={}, email={}", entity.getId(), entity.getEmail());

        // 2. Normaliser le téléphone et l'email
        String telephone = request.getPhoneNumber();
        if (telephone != null && !telephone.isBlank()) {
            telephone = normalisationService.normaliseTelephone(telephone);
        }
        String email = normalisationService.normaliseEmail(request.getEmail());

        // 3. Vérifier l'unicité du téléphone et email sur gestion_account
        if (telephone != null && gestionAccountRepository.existsByTelephone(telephone)) {
            throw new BusinessException("Un compte existe déjà avec le numéro de téléphone '" + telephone + "'");
        }
        if (gestionAccountRepository.existsByEmail(email)) {
            throw new BusinessException("Un compte existe déjà avec l'email '" + email + "'");
        }

        // 4. Générer le mot de passe temporaire
        String temporaryPassword = temporaryPasswordService.generateTemporaryPassword();
        String hashedPassword = passwordEncoder.encode(temporaryPassword);

        // 5. Créer le compte GestionAccount avec le rôle RESPONSABLE
        GestionAccount account = GestionAccount.builder()
                .fullName(entity.getFirstName() + " " + entity.getLastName())
                .telephone(telephone != null ? telephone : "")
                .email(email)
                .password(hashedPassword)
                .role(Role.ROLE_RESPONSIBLE)
                .active(true)
                .mustChangePassword(true)
                .build();

        try {
            account = gestionAccountRepository.save(account);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            String message = "Impossible de créer le compte : identifiant déjà utilisé";
            String lower = e.getMessage();
            if (lower.contains("telephone") || lower.contains("uk_gestion_account_telephone")) {
                message = "Un compte existe déjà avec ce numéro de téléphone";
            } else if (lower.contains("email") || lower.contains("uk_gestion_account_email")) {
                message = "Un compte existe déjà avec cette adresse email";
            }
            throw new BusinessException(message);
        }
        log.info("Compte GestionAccount créé pour le responsable : id={}, email={}",
                account.getId(), account.getEmail());

        // 6. Envoyer l'email avec le mot de passe temporaire
        sendWelcomeEmail(entity, telephone, temporaryPassword);

        return responsibleMapper.toResponse(entity);
    }

    /**
     * Envoie l'email de bienvenue avec le mot de passe temporaire.
     *
     * @param responsible       entité Responsible (nom, email)
     * @param normalizedTelephone téléphone normalisé (forme canonique utilisée pour la connexion)
     * @param temporaryPassword mot de passe temporaire à transmettre
     */
    private void sendWelcomeEmail(Responsible responsible, String normalizedTelephone, String temporaryPassword) {
        String subject = "Bienvenue sur le Gestion Service - Vos identifiants de connexion";

        String htmlContent = """
                <html>
                <body style="font-family: Arial, sans-serif; color: #333;">
                    <h2 style="color: #2c3e50;">Bienvenue %s %s !</h2>
                    <p>Votre compte a été créé par l'administrateur du Gestion Service.</p>
                    <p>Pour accéder à la plateforme, utilisez les identifiants suivants :</p>
                    <ul>
                        <li><strong>Numéro de téléphone :</strong> <code style="background:#f0f0f0; padding:4px 8px; border-radius:4px;">%s</code></li>
                        <li><strong>Email :</strong> %s</li>
                        <li><strong>Mot de passe temporaire :</strong> <code style="background:#f0f0f0; padding:4px 8px; border-radius:4px;">%s</code></li>
                    </ul>
                    <p style="color: #e67e22;"><strong>Important :</strong> Pour des raisons de sécurité, vous devez changer ce mot de passe temporaire lors de votre première connexion.</p>
                    <p style="font-size: 0.9em; color: #666;">Utilisez votre numéro de téléphone et votre mot de passe temporaire pour vous connecter, puis rendez-vous sur <code>POST /api/v1/auth/first-change-password</code> pour définir un nouveau mot de passe.</p>
                    <p>Cordialement,<br>L'équipe Gestion Service</p>
                </body>
                </html>
                """.formatted(
                responsible.getFirstName(),
                responsible.getLastName(),
                normalizedTelephone != null ? normalizedTelephone : "(à contacter)",
                responsible.getEmail(),
                temporaryPassword
        );

        emailService.sendEmail(responsible.getEmail(), subject, htmlContent);
    }
}