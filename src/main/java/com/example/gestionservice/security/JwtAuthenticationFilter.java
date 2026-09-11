package com.example.gestionservice.security;

import com.example.gestionservice.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Filtre JWT — extrait et valide le Bearer token à chaque requête.
 * Peuple le SecurityContext avec JwtAuthenticationPrincipal (accountId + roles).
 * NE JAMAIS logger le token complet.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Claims claims = jwtService.validateAndGetClaims(token);

                if (jwtService.isTokenExpired(claims)) {
                    log.debug("JWT expiré pour la requête : {}", request.getRequestURI());
                    filterChain.doFilter(request, response);
                    return;
                }

                UUID accountId = jwtService.extractAccountId(claims);
                List<Role> roles = jwtService.extractRoles(claims);

                JwtAuthenticationPrincipal principal = new JwtAuthenticationPrincipal(accountId, roles);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                principal.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("JWT valide — accountId={}, rôles={}", accountId, roles);

            } catch (JwtException e) {
                // Token invalide — on ne logue pas le token, uniquement le message d'erreur
                log.warn("JWT invalide : {}", e.getMessage());
            } catch (Exception e) {
                log.warn("Erreur lors du traitement JWT : {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
