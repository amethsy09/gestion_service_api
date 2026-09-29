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
import java.util.UUID;

/**
 * Filtre JWT — valide le jeton local et résout l'identité à chaque requête.
 *
 * <p>Flux (Étape 5 — cutover définitif {@code telephone → UUID}) :</p>
 * <pre>
 * Authorization: Bearer JWT
 *     ↓  validateLocalToken : signature + expiration + issuer
 * claims.sub (UUID)
 *     ↓  extractAccountId
 * AccountSecurityResolver.resolve(accountId)
 *     ↓  gestion_account par id : existence + active + rôle + téléphone
 * JwtAuthenticationPrincipal
 *     ↓  SecurityContext
 * </pre>
 *
 * <p>Seul le format local est accepté : {@code sub} doit être l'UUID d'un
 * {@code gestion_account} existant et actif, et le jeton doit porter
 * {@code iss = jwt.issuer}.</p>
 *
 * <p>Sont rejetés (donc conduit à une réponse 401 par
 * {@link RestAuthenticationEntryPoint}, la requête restant anonyme) :</p>
 * <ul>
 *   <li>un jeton {@code sub = telephone} (ancien format, sans émetteur) ;</li>
 *   <li>un {@code sub} qui n'est pas un UUID ;</li>
 *   <li>un UUID valide mais sans compte correspondant ;</li>
 *   <li>un compte {@code active = false} ;</li>
 *   <li>un jeton expiré, mal signé, ou dont l'émetteur est incorrect.</li>
 * </ul>
 *
 * <p><b>Aucun compte n'est jamais créé depuis ce filtre.</b> La résolution est
 * une lecture seule de {@code gestion_account} ; le rôle appliqué aux
 * autorisations est celui lu en base, jamais une valeur fournie par le client
 * ou portée par le jeton.</p>
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
                // 1-2. Signature, expiration et émetteur. L'expiration est
                // vérifiée par le parseur lui-même : un jeton hors délai lève
                // une ExpiredJwtException ici, il n'y a plus de test séparé.
                Claims claims = jwtService.validateLocalToken(token);

                // 3-4. sub → UUID (un sub non UUID est rejeté explicitement).
                UUID accountId = jwtService.extractAccountId(claims);

                // 5-8. Résolution en base : existence, active = true, rôle,
                //       téléphone. Aucune création de compte à ce niveau.
                AccountSecurityInfo securityInfo = accountSecurityResolver.resolve(accountId);

                JwtAuthenticationPrincipal principal = new JwtAuthenticationPrincipal(
                        securityInfo.accountId(),
                        securityInfo.roles(),
                        securityInfo.telephone());

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                principal.getAuthorities());

                // 9. Publication dans le SecurityContext.
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("JWT local valide — accountId={}, telephone={}, rôles={}",
                        securityInfo.accountId(), securityInfo.telephone(), securityInfo.roles());

            } catch (JwtException e) {
                log.warn("JWT rejeté : {}", e.getMessage());
            } catch (Exception e) {
                // Dont UnauthorizedException : UUID inconnu ou compte désactivé.
                log.warn("Authentification refusée : {}", e.getMessage());
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
