package com.example.gestionservice.exception;

/**
 * Levée lorsqu'une contrainte d'unicité ou d'intégrité est violée
 * (ex : téléphone ou email déjà utilisé).
 *
 * <p>Traduit en HTTP 409 (Conflict) par
 * {@link com.example.gestionservice.exception.GlobalExceptionHandler}.</p>
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
