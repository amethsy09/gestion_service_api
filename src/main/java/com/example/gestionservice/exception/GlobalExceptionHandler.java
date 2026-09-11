package com.example.gestionservice.exception;

import com.example.gestionservice.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Gestionnaire centralisé des exceptions.
 * Retourne un format ErrorResponse standardisé pour toutes les erreurs.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ============================================================
    //  404 — Ressource introuvable
    // ============================================================

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Ressource introuvable : {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), request.getRequestURI(), null);
    }

    // ============================================================
    //  400 — Règles métier
    // ============================================================

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(
            BusinessException ex, HttpServletRequest request) {
        log.warn("Erreur métier : {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "BUSINESS_ERROR", ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            ValidationException ex, HttpServletRequest request) {
        log.warn("Erreur de validation : {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage(), request.getRequestURI(), null);
    }

    // ============================================================
    //  400 — Bean Validation (@Valid)
    // ============================================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<ErrorResponse.ValidationError> errors = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> {
                    if (error instanceof FieldError fieldError) {
                        return ErrorResponse.ValidationError.builder()
                                .field(fieldError.getField())
                                .message(fieldError.getDefaultMessage())
                                .build();
                    }
                    return ErrorResponse.ValidationError.builder()
                            .field(error.getObjectName())
                            .message(error.getDefaultMessage())
                            .build();
                })
                .toList();

        log.warn("Validation Bean échouée : {} erreur(s)", errors.size());
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Erreur(s) de validation des données", request.getRequestURI(), errors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = "Paramètre invalide : '" + ex.getName() + "' — valeur attendue : "
                + (ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "inconnue");
        return build(HttpStatus.BAD_REQUEST, "TYPE_MISMATCH", message, request.getRequestURI(), null);
    }

    // ============================================================
    //  402 / 409 — Paiement
    // ============================================================

    @ExceptionHandler(PaymentAlreadyProcessedException.class)
    public ResponseEntity<ErrorResponse> handlePaymentAlreadyProcessed(
            PaymentAlreadyProcessedException ex, HttpServletRequest request) {
        log.warn("Paiement déjà traité : {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, "PAYMENT_ALREADY_PROCESSED",
                ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ErrorResponse> handlePayment(
            PaymentException ex, HttpServletRequest request) {
        log.error("Erreur de paiement : {}", ex.getMessage());
        return build(HttpStatus.PAYMENT_REQUIRED, "PAYMENT_ERROR",
                ex.getMessage(), request.getRequestURI(), null);
    }

    // ============================================================
    //  502 — Erreur communication Wallet
    // ============================================================

    @ExceptionHandler(WalletCommunicationException.class)
    public ResponseEntity<ErrorResponse> handleWalletCommunication(
            WalletCommunicationException ex, HttpServletRequest request) {
        log.error("Erreur communication Wallet : {}", ex.getMessage());
        return build(HttpStatus.BAD_GATEWAY, "WALLET_COMMUNICATION_ERROR",
                ex.getMessage(), request.getRequestURI(), null);
    }

    // ============================================================
    //  401 / 403 — Sécurité
    // ============================================================

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(
            UnauthorizedException ex, HttpServletRequest request) {
        log.warn("Non autorisé : {}", ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(
            ForbiddenException ex, HttpServletRequest request) {
        log.warn("Accès interdit : {}", ex.getMessage());
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN",
                ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Accès refusé Spring Security : {}", request.getRequestURI());
        return build(HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                "Vous n'avez pas les droits nécessaires pour cette opération",
                request.getRequestURI(), null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
            AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentification échouée : {}", request.getRequestURI());
        return build(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED",
                "Authentification requise — fournissez un JWT valide dans le header Authorization",
                request.getRequestURI(), null);
    }

    // ============================================================
    //  500 — Erreur inattendue
    // ============================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest request) {
        log.error("Erreur inattendue sur {} : {}", request.getRequestURI(), ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Une erreur interne s'est produite. Veuillez réessayer ultérieurement.",
                request.getRequestURI(), null);
    }

    // ============================================================
    //  Builder interne
    // ============================================================

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status,
            String error,
            String message,
            String path,
            List<ErrorResponse.ValidationError> errors) {

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(error)
                .message(message)
                .path(path)
                .errors(errors)
                .build();

        return ResponseEntity.status(status).body(body);
    }
}
