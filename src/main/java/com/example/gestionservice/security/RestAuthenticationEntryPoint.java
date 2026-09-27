package com.example.gestionservice.security;

import com.example.gestionservice.dto.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Point d'entrée d'authentification REST.
 *
 * <p>Lorsqu'une requête anonyme atteint un endpoint protégé,
 * Spring Security appelle cette classe au lieu de renvoyer la page de
 * connexion HTML par défaut.</p>
 *
 * <p>Elle retourne un JSON HTTP 401 au format {@link ErrorResponse}.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        log.warn("Accès refusé à {} : authentification requise", request.getRequestURI());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpServletResponse.SC_UNAUTHORIZED)
                .error("AUTHENTICATION_REQUIRED")
                .message("Authentification requise — fournissez un JWT valide dans le header Authorization")
                .path(request.getRequestURI())
                .build();

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}