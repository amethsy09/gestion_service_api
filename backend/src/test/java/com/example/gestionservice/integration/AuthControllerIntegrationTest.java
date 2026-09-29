package com.example.gestionservice.integration;

import com.example.gestionservice.AbstractIntegrationTest;
import com.example.gestionservice.dto.request.LoginRequest;
import com.example.gestionservice.dto.request.RegisterRequest;
import com.example.gestionservice.dto.response.AuthResponse;
import com.example.gestionservice.entity.GestionAccount;
import com.example.gestionservice.enums.Role;
import com.example.gestionservice.repository.GestionAccountRepository;
import com.example.gestionservice.security.JwtService;
import com.example.gestionservice.support.JwtTestTokenFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration — inscription, connexion et sécurité des endpoints d'auth.
 *
 * <p>Couvre l'ensemble des scénarios de l'Étape 3 : validation Bean Validation,
 * normalisation téléphone/email, unicité (409), hash BCrypt, génération JWT local
 * (sub = gestion_account.id), et contrats 401/403.</p>
 */
@AutoConfigureMockMvc
@DisplayName("AuthController — Tests d'intégration (inscription / connexion)")
class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String VALID_PASSWORD = "motdepasse123";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired GestionAccountRepository gestionAccountRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;

    @Value("${jwt.secret}") String jwtSecret;

    private JwtTestTokenFactory tokenFactory;

    @BeforeEach
    void setUp() {
        tokenFactory = new JwtTestTokenFactory(jwtSecret);
    }

    // ==================================================================
    //  Helpers
    // ==================================================================

    /** Téléphone canonique unique (9 chiffres commençant par 77). */
    private String canonicalTelephone() {
        return JwtTestTokenFactory.uniqueTelephone();
    }

    /** RegisterRequest valide, avec un téléphone déjà sous forme canonique. */
    private RegisterRequest validRegisterRequest(String telephone) {
        return RegisterRequest.builder()
                .fullName("Alassane Diallo")
                .telephone(telephone)
                .email("user-" + telephone + "@example.com")
                .password(VALID_PASSWORD)
                .build();
    }

    /** Serialise un DTO en JSON. */
    private String toJson(Object dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    /** Enregistre un utilisateur via l'API et retourne la réponse. */
    private AuthResponse register(String telephone, String email, String password) throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .fullName("Alassane Diallo")
                .telephone(telephone)
                .email(email)
                .password(password)
                .build();

        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(body, AuthResponse.class);
    }

    /** Enregistre un utilisateur simple et retourne son téléphone canonique. */
    private String registerDefaultUser() throws Exception {
        String tel = canonicalTelephone();
        register(tel, "user-" + tel + "@example.com", VALID_PASSWORD);
        return tel;
    }

    // ==================================================================
    //  REGISTER — scénarios de validation Bean Validation
    // ==================================================================

    @Test
    @DisplayName("Register — inscription valide → 200 avec token et rôle ROLE_USER")
    void register_valid_returns200WithTokenAndRole() throws Exception {
        String tel = canonicalTelephone();
        RegisterRequest req = validRegisterRequest(tel);

        AuthResponse authResponse = register(tel, "user-" + tel + "@example.com", VALID_PASSWORD);

        assertThat(authResponse.getToken()).isNotBlank();
        assertThat(authResponse.getRole()).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("Register — fullName vide → 400")
    void register_fullNameBlank_rejected400() throws Exception {
        RegisterRequest req = validRegisterRequest(canonicalTelephone());
        req.setFullName("");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Register — fullName espaces uniquement → 400")
    void register_fullNameSpacesOnly_rejected400() throws Exception {
        RegisterRequest req = validRegisterRequest(canonicalTelephone());
        req.setFullName("   ");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Register — téléphone vide → 400")
    void register_telephoneBlank_rejected400() throws Exception {
        RegisterRequest req = validRegisterRequest(canonicalTelephone());
        req.setTelephone("");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Register — email vide → 400")
    void register_emailBlank_rejected400() throws Exception {
        RegisterRequest req = validRegisterRequest(canonicalTelephone());
        req.setEmail("");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Register — password vide → 400")
    void register_passwordBlank_rejected400() throws Exception {
        RegisterRequest req = validRegisterRequest(canonicalTelephone());
        req.setPassword("");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Register — password < 8 caractères → 400")
    void register_passwordTooShort_rejected400() throws Exception {
        RegisterRequest req = validRegisterRequest(canonicalTelephone());
        req.setPassword("1234567");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("Register — password > 72 caractères → 400")
    void register_passwordTooLong_rejected400() throws Exception {
        RegisterRequest req = validRegisterRequest(canonicalTelephone());
        req.setPassword("a".repeat(73));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Register — password de 72 caractères → accepté")
    void register_passwordExactly72_accepted() throws Exception {
        String tel = canonicalTelephone();
        RegisterRequest req = RegisterRequest.builder()
                .fullName("Alassane Diallo")
                .telephone(tel)
                .email("user-" + tel + "@example.com")
                .password("a".repeat(72))
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Register — email invalide → 400")
    void register_invalidEmail_rejected400() throws Exception {
        RegisterRequest req = validRegisterRequest(canonicalTelephone());
        req.setEmail("not-an-email");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Register — rôle/envoi de role par le client ignoré (toujours ROLE_USER)")
    void register_clientCannotSetRole() throws Exception {
        String tel = canonicalTelephone();
        // Même si le client tente d'envoyer d'autres champs, seuls
        // fullName, telephone, email, password sont dans le DTO.
        String body = """
                {
                  "fullName": "Alassane Diallo",
                  "telephone": "%s",
                  "email": "user-%s@example.com",
                  "password": "%s"
                }
                """.formatted(tel, tel, VALID_PASSWORD);

        AuthResponse authResponse = objectMapper.readValue(
                mockMvc.perform(post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                AuthResponse.class);

        assertThat(authResponse.getRole()).isEqualTo("ROLE_USER");

        GestionAccount account = gestionAccountRepository.findByTelephone(tel).orElseThrow();
        assertThat(account.getRole()).isEqualTo(Role.ROLE_USER);
    }

    // ==================================================================
    //  REGISTER — normalisation téléphone / email
    // ==================================================================

    @Test
    @DisplayName("Register — email normalisé en minuscules")
    void register_emailNormalizedToLowercase() throws Exception {
        String tel = canonicalTelephone();
        String rawEmail = "User-" + tel + "@Example.COM";
        String expectedNormalized = rawEmail.toLowerCase();

        register(tel, rawEmail, VALID_PASSWORD);

        GestionAccount account = gestionAccountRepository.findByEmail(expectedNormalized).orElseThrow();
        assertThat(account.getEmail()).isEqualTo(expectedNormalized);
    }

    @Test
    @DisplayName("Register — téléphone normalisé (+221 → canonique)")
    void register_telephoneNormalizedFromInternational() throws Exception {
        String canonical = canonicalTelephone();
        String rawPhone = "+221 " + canonical;

        register(rawPhone, "user-" + canonical + "@example.com", VALID_PASSWORD);

        GestionAccount account = gestionAccountRepository.findByTelephone(canonical).orElseThrow();
        assertThat(account.getTelephone()).isEqualTo(canonical);
    }

    @Test
    @DisplayName("Register — téléphone normalisé (00221 → canonique)")
    void register_telephoneNormalizedFrom00221() throws Exception {
        String canonical = canonicalTelephone();
        String rawPhone = "00221 " + canonical;

        register(rawPhone, "user2-" + canonical + "@example.com", VALID_PASSWORD);

        GestionAccount account = gestionAccountRepository.findByTelephone(canonical).orElseThrow();
        assertThat(account.getTelephone()).isEqualTo(canonical);
    }

    // ==================================================================
    //  REGISTER — unicité (409)
    // ==================================================================

    @Test
    @DisplayName("Register — téléphone déjà utilisé → 409")
    void register_telephoneAlreadyUsed_returns409() throws Exception {
        String tel = canonicalTelephone();
        register(tel, "user-" + tel + "@example.com", VALID_PASSWORD);

        RegisterRequest duplicate = validRegisterRequest(tel);
        duplicate.setEmail("other-" + tel + "@example.com");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Ce numéro de téléphone est déjà utilisé"));
    }

    @Test
    @DisplayName("Register — email déjà utilisé → 409")
    void register_emailAlreadyUsed_returns409() throws Exception {
        String tel = canonicalTelephone();
        String email = "user-" + tel + "@example.com";
        register(tel, email, VALID_PASSWORD);

        String otherTel = canonicalTelephone();
        RegisterRequest duplicate = validRegisterRequest(otherTel);
        duplicate.setEmail(email);
        // Utilise une majuscule : la normalisation doit la convertir, puis conflter
        duplicate.setEmail(email.toUpperCase());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Cette adresse email est déjà utilisée"));
    }

    // ==================================================================
    //  REGISTER — persistance et BCrypt
    // ==================================================================

    @Test
    @DisplayName("Register — compte créé avec active = true")
    void register_accountCreatedWithActiveTrue() throws Exception {
        String tel = canonicalTelephone();

        register(tel, "user-" + tel + "@example.com", VALID_PASSWORD);

        GestionAccount account = gestionAccountRepository.findByTelephone(tel).orElseThrow();
        assertThat(account.getActive()).isTrue();
    }

    @Test
    @DisplayName("Register — rôle forcé à ROLE_USER en base")
    void register_roleForcedToUserRoleInDb() throws Exception {
        String tel = canonicalTelephone();

        register(tel, "user-" + tel + "@example.com", VALID_PASSWORD);

        GestionAccount account = gestionAccountRepository.findByTelephone(tel).orElseThrow();
        assertThat(account.getRole()).isEqualTo(Role.ROLE_USER);
    }

    @Test
    @DisplayName("Register — mot de passe stocké sous forme BCrypt")
    void register_passwordStoredAsBcrypt() throws Exception {
        String tel = canonicalTelephone();

        register(tel, "user-" + tel + "@example.com", VALID_PASSWORD);

        GestionAccount account = gestionAccountRepository.findByTelephone(tel).orElseThrow();
        assertThat(account.getPassword()).startsWith("$2a$12$");
        assertThat(account.getPassword()).hasSize(60);
        assertThat(account.getPassword()).doesNotContain(VALID_PASSWORD);
        assertThat(passwordEncoder.matches(VALID_PASSWORD, account.getPassword())).isTrue();
        assertThat(passwordEncoder.matches("mauvais", account.getPassword())).isFalse();
    }

    @Test
    @DisplayName("Register — aucun password/hash dans AuthResponse")
    void register_noPasswordInResponse() throws Exception {
        String responseBody = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(validRegisterRequest(canonicalTelephone()))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Le JSON ne contient que token et role.
        assertThat(responseBody).doesNotContain("password");
        assertThat(responseBody).doesNotContain("$2a$");

        AuthResponse authResponse = objectMapper.readValue(responseBody, AuthResponse.class);
        assertThat(authResponse.getToken()).isNotBlank();
        assertThat(authResponse.getRole()).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("Register — JWT contient l'UUID du compte gestion_account")
    void register_jwtContainsAccountUuid() throws Exception {
        String tel = canonicalTelephone();
        String email = "user-" + tel + "@example.com";

        AuthResponse authResponse = register(tel, email, VALID_PASSWORD);

        Claims claims = jwtService.validateLocalToken(authResponse.getToken());
        UUID accountId = jwtService.extractAccountId(claims);

        GestionAccount account = gestionAccountRepository.findById(accountId).orElseThrow();
        assertThat(account.getTelephone()).isEqualTo(tel);
    }

    // ==================================================================
    //  LOGIN
    // ==================================================================

    @Test
    @DisplayName("Login — connexion valide → 200 avec token et rôle")
    void login_valid_returns200WithTokenAndRole() throws Exception {
        String tel = registerDefaultUser();

        String body = toJson(LoginRequest.builder()
                .telephone(tel)
                .password(VALID_PASSWORD)
                .build());

        String responseBody = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("ROLE_USER"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        AuthResponse authResponse = objectMapper.readValue(responseBody, AuthResponse.class);

        // Le JWT contient l'UUID du compte.
        Claims claims = jwtService.validateLocalToken(authResponse.getToken());
        UUID accountId = jwtService.extractAccountId(claims);
        GestionAccount account = gestionAccountRepository.findById(accountId).orElseThrow();
        assertThat(account.getTelephone()).isEqualTo(tel);
    }

    @Test
    @DisplayName("Login — mauvais mot de passe → 401")
    void login_wrongPassword_returns401() throws Exception {
        String tel = registerDefaultUser();

        String body = toJson(LoginRequest.builder()
                .telephone(tel)
                .password("mauvais-mot-de-passe")
                .build());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Login — téléphone inexistant → 401")
    void login_telephoneNotFound_returns401() throws Exception {
        String tel = canonicalTelephone();

        String body = toJson(LoginRequest.builder()
                .telephone(tel)
                .password(VALID_PASSWORD)
                .build());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Login — compte désactivé → 401")
    void login_disabledAccount_returns401() throws Exception {
        String tel = canonicalTelephone();
        gestionAccountRepository.save(GestionAccount.builder()
                .fullName("Compte Désactivé")
                .telephone(tel)
                .email("disabled-" + tel + "@example.com")
                .password(passwordEncoder.encode(VALID_PASSWORD))
                .role(Role.ROLE_USER)
                .active(false)
                .build());

        String body = toJson(LoginRequest.builder()
                .telephone(tel)
                .password(VALID_PASSWORD)
                .build());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Login — téléphone normalisé (+221) → connexion réussit")
    void login_telephoneNormalized() throws Exception {
        String canonical = registerDefaultUser();

        String body = toJson(LoginRequest.builder()
                .telephone("+221 " + canonical)
                .password(VALID_PASSWORD)
                .build());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    @Test
    @DisplayName("Login — JWT local contient l'UUID du compte")
    void login_jwtContainsAccountUuid() throws Exception {
        String tel = registerDefaultUser();

        String body = toJson(LoginRequest.builder()
                .telephone(tel)
                .password(VALID_PASSWORD)
                .build());

        String responseBody = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        AuthResponse authResponse = objectMapper.readValue(responseBody, AuthResponse.class);
        Claims claims = jwtService.validateLocalToken(authResponse.getToken());
        UUID accountId = jwtService.extractAccountId(claims);

        GestionAccount account = gestionAccountRepository.findById(accountId).orElseThrow();
        assertThat(account.getTelephone()).isEqualTo(tel);
    }

    @Test
    @DisplayName("Login — aucun password/hash dans AuthResponse")
    void login_noPasswordInResponse() throws Exception {
        String tel = registerDefaultUser();

        String responseBody = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(LoginRequest.builder()
                                .telephone(tel)
                                .password(VALID_PASSWORD)
                                .build())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseBody).doesNotContain("password");
        assertThat(responseBody).doesNotContain("$2a$");
    }

    // ==================================================================
    //  SÉCURITÉ
    // ==================================================================

    @Test
    @DisplayName("Security — POST /api/v1/auth/register accessible sans JWT")
    void register_accessibleWithoutJwt() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(validRegisterRequest(canonicalTelephone()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("Security — POST /api/v1/auth/login accessible sans JWT")
    void login_accessibleWithoutJwt() throws Exception {
        String tel = registerDefaultUser();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(LoginRequest.builder()
                                .telephone(tel)
                                .password(VALID_PASSWORD)
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("Security — endpoint protégé sans JWT → 401 JSON")
    void protectedEndpoint_withoutJwt_returns401Json() throws Exception {
        mockMvc.perform(get("/api/v1/service-requests/my"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("Security — endpoint protégé avec JWT local mais sans permission → 403 JSON")
    void protectedEndpoint_withJwtButNoPermission_returns403Json() throws Exception {
        String tel = canonicalTelephone();
        GestionAccount account = gestionAccountRepository.saveAndFlush(GestionAccount.builder()
                .fullName("Regular User")
                .telephone(tel)
                .email("user-" + tel + "@example.com")
                .role(Role.ROLE_USER)
                .active(true)
                .build());
        // Format local depuis l'Étape 5 : sub = gestion_account.id.
        String userJwt = tokenFactory.localTokenForAccount(account);

        mockMvc.perform(get("/api/v1/admin/service-requests")
                        .header("Authorization", "Bearer " + userJwt))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}
