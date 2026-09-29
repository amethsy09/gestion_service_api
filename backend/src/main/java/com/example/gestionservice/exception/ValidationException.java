package com.example.gestionservice.exception;

/**
 * Levée lors d'une erreur de validation métier (données, unicité, etc.).
 *
 * <p>Traduit en HTTP 400 par {@link com.example.gestionservice.exception.GlobalExceptionHandler}.</p>
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}