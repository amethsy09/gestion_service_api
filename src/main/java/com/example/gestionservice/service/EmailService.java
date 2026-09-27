package com.example.gestionservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

/**
 * Service d'envoi d'emails transactionnels via Gmail SMTP (JavaMailSender).
 *
 * <p>L'envoi utilise {@code spring.mail.*} configuré dans
 * {@code application.yml} / {@code application-secrets.yml},
 * avec authentification STARTTLS sur {@code smtp.gmail.com:587}.</p>
 *
 * <p>Si le {@code username} (adresse Gmail) n'est pas configuré ou si
 * l'envoi échoue (ex: mot de passe d'application invalide, service
 * SMTP indisponible), l'envoi est silencieusement réduit à un log
 * (mode dégradé, utile en développement et en production).
 * L'erreur n'est jamais propagée : l'envoi d'email est best-effort
 * et ne doit jamais entraîner de rollback de la création de compte.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    /**
     * Envoie un email transactionnel.
     *
     * @param to          adresse email du destinataire
     * @param subject     objet de l'email
     * @param htmlContent contenu HTML de l'email
     */
    public void sendEmail(String to, String subject, String htmlContent) {
        String username = mailProperties.getUsername();
        if (username == null || username.isBlank()) {
            log.warn("Mail username non configuré — email non envoyé. Destinataire={}, Objet={}", to, subject);
            log.debug("Contenu HTML de l'email : {}", htmlContent);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(username);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("Email envoyé avec succès via Gmail SMTP : destinataire={}, objet={}", to, subject);
        } catch (Exception e) {
            log.warn("Échec envoi email via Gmail SMTP vers {} : {} — le compte est néanmoins créé", to, e.getMessage());
        }
    }
}
