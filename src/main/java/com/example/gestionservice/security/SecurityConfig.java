package com.example.gestionservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Configuration Spring Security.
 * - STATELESS (JWT, pas de session)
 * - CORS activé pour les frontends autorisés
 * - CSRF désactivé (API REST)
 * - Swagger et Actuator health accessibles sans auth
 * - @PreAuthorize activé via @EnableMethodSecurity
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String[] PUBLIC_PATHS = {
            // Swagger UI
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/v3/api-docs",
            // Actuator health
            "/actuator/health",
            "/actuator/info",
    };

    /**
     * Configuration CORS pour les frontends autorisés.
     * Les origines peuvent être configurées par la variable d'environnement
     * CORS_ALLOWED_ORIGINS (liste séparée par des virgules).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        String envOrigins = System.getenv("CORS_ALLOWED_ORIGINS");
        List<String> origins;
        if (envOrigins != null && !envOrigins.isBlank()) {
            origins = Arrays.stream(envOrigins.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        } else {
            origins = List.of(
                    "http://localhost:3000",
                    "http://localhost:4200",
                    "http://localhost:5173"
            );
        }

        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Requested-With",
                "Idempotency-Key"
        ));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(c -> c.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Swagger + Actuator ouverts
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        // GET catalogue de services accessible sans auth
                        .requestMatchers(HttpMethod.GET, "/api/v1/services/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/specialties/**").permitAll()
                        // Tout le reste nécessite une authentification
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
