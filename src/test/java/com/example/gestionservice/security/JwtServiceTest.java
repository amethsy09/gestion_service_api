package com.example.gestionservice.security;

import com.example.gestionservice.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService — Tests unitaires")
class JwtServiceTest {

    private static final String SECRET = "test_secret_key_for_junit_tests_32_chars_min";
    private JwtService jwtService;
    private SecretKey signingKey;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET);
        signingKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    // ================================================================
    //  JWT valide
    // ================================================================

    @Test
    @DisplayName("JWT valide — extractTelephone retourne le sub")
    void extractTelephone_validJwt_returnsSub() {
        String token = Jwts.builder()
                .setSubject("771234567")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();

        Claims claims = jwtService.validateAndGetClaims(token);
        String telephone = jwtService.extractTelephone(claims);

        assertThat(telephone).isEqualTo("771234567");
    }

    @Test
    @DisplayName("JWT valide — isTokenExpired retourne false")
    void isTokenExpired_validJwt_returnsFalse() {
        String token = Jwts.builder()
                .setSubject("771234567")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();

        Claims claims = jwtService.validateAndGetClaims(token);

        assertThat(jwtService.isTokenExpired(claims)).isFalse();
    }

    // ================================================================
    //  JWT expiré
    // ================================================================

    @Test
    @DisplayName("JWT expiré — validateAndGetClaims lance JwtException")
    void validateAndGetClaims_expiredToken_throwsJwtException() {
        String token = Jwts.builder()
                .setSubject("771234567")
                .setIssuedAt(new Date(System.currentTimeMillis() - 7_200_000))
                .setExpiration(new Date(System.currentTimeMillis() - 3_600_000))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();

        assertThatThrownBy(() -> jwtService.validateAndGetClaims(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("JWT expiré — isTokenExpired retourne true")
    void isTokenExpired_expiredJwt_returnsTrue() {
        String token = Jwts.builder()
                .setSubject("771234567")
                .setIssuedAt(new Date(System.currentTimeMillis() - 7_200_000))
                .setExpiration(new Date(System.currentTimeMillis() - 3_600_000))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();

        // validateAndGetClaims throws ExpiredJwtException for expired tokens;
        // retrieve claims from the exception body instead.
        Claims claims;
        try {
            jwtService.validateAndGetClaims(token);
            throw new AssertionError("Expected ExpiredJwtException");
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            claims = e.getClaims();
        }

        assertThat(jwtService.isTokenExpired(claims)).isTrue();
    }

    // ================================================================
    //  JWT signature invalide
    // ================================================================

    @Test
    @DisplayName("JWT signature invalide — validateAndGetClaims lance JwtException")
    void validateAndGetClaims_invalidSignature_throwsJwtException() {
        SecretKey wrongKey = Keys.hmacShaKeyFor("another_wrong_secret_key_32_chars_min".getBytes(StandardCharsets.UTF_8));

        String token = Jwts.builder()
                .setSubject("771234567")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(wrongKey, SignatureAlgorithm.HS256)
                .compact();

        assertThatThrownBy(() -> jwtService.validateAndGetClaims(token))
                .isInstanceOf(JwtException.class);
    }

    // ================================================================
    //  JWT sans sub
    // ================================================================

    @Test
    @DisplayName("JWT sans sub — extractTelephone lance JwtException")
    void extractTelephone_jwWithoutSub_throwsJwtException() {
        String token = Jwts.builder()
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();

        Claims claims = jwtService.validateAndGetClaims(token);

        assertThatThrownBy(() -> jwtService.extractTelephone(claims))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("sub");
    }

    // ================================================================
    //  JWT avec sub vide
    // ================================================================

    @Test
    @DisplayName("JWT avec sub vide — extractTelephone lance JwtException")
    void extractTelephone_jwWithEmptySub_throwsJwtException() {
        String token = Jwts.builder()
                .setSubject(" ")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();

        Claims claims = jwtService.validateAndGetClaims(token);

        assertThatThrownBy(() -> jwtService.extractTelephone(claims))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("sub");
    }
}
