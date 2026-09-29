package com.example.gestionservice.security;

import com.example.gestionservice.dto.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Gestionnaire d'accès refusé REST.
 *
 * <p>Lorsqu'un utilisateur authentifié tente d'accéder à une ressource
 * pour laquelle il n'a pas les permissions, Spring Security appelle cette
 * classe au lieu de renvoyer une page d'erreur HTML.</p>
 *
 * <p>Elle retourne un JSON HTTP 403 au format {@link ErrorResponse}.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException ex) throws IOException {

        log.warn("Accès refusé à {} : permissions insuffisantes", request.getRequestURI());

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpServletResponse.SC_FORBIDDEN)
                .error("ACCESS_DENIED")
                .message("Vous n'avez pas les droits nécessaires pour cette opération")
                .path(request.getRequestURI())
                .build();

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}