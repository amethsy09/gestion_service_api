package com.example.gestionservice.exception;

/**
 * Levée au démarrage lorsque les informations configurées pour le compte
 * ADMIN initial entrent en conflit avec un compte existant.
 *
 * <p>Contexte : aucun ADMIN n'existe, mais le téléphone ou l'email configuré
 * est déjà porté par un autre compte. Le seeder ne doit NI écraser ce compte,
 * NI modifier son rôle : il interrompt le démarrage avec cette erreur
 * explicite pour que l'opérateur corrige sa configuration.</p>
 *
 * <p>Exception de démarrage uniquement : elle n'est pas traduite en réponse
 * HTTP par le {@link GlobalExceptionHandler}, car aucun endpoint ne la déclenche.</p>
 */
public class AdminSeedConflictException extends RuntimeException {

    public AdminSeedConflictException(String message) {
        super(message);
    }

    public AdminSeedConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
