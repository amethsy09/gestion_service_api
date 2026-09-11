package com.example.gestionservice.security;

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

/**
 * Filtre JWT — extrait et valide le Bearer token à chaque requête.
 *
 * <p>Flux :</p>
 * <pre>
 * Authorization: Bearer JWT
 *     ↓  validation signature + expiration
 * claims.sub
 *     ↓  téléphone
 * AccountSecurityResolver.resolve(telephone)
 *     ↓  gestion_account → UUID interne + rôles
 * JwtAuthenticationPrincipal
 *     ↓  SecurityContext
 * </pre>
 *
 * <p>NE JAMAIS logger le token JWT complet ni le PIN.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final AccountSecurityResolver accountSecurityResolver;

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

                String telephone = jwtService.extractTelephone(claims);
                AccountSecurityInfo securityInfo = accountSecurityResolver.resolve(telephone);

                JwtAuthenticationPrincipal principal = new JwtAuthenticationPrincipal(
                        securityInfo.accountId(),
                        securityInfo.roles(),
                        securityInfo.telephone());

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                principal.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("JWT valide — telephone={}, accountId={}, rôles={}",
                        telephone, securityInfo.accountId(), securityInfo.roles());

            } catch (JwtException e) {
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
