package com.example.gestionservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Format standard des réponses d'erreur de l'API.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    private int status;
    private String error;
    private String message;
    private String path;

    /** Détail des erreurs de validation (champ → message). */
    private List<ValidationError> errors;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationError {
        private String field;
        private String message;
    }
}
