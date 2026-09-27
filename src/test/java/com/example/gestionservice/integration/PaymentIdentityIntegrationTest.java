package com.example.gestionservice.integration;

import com.example.gestionservice.AbstractIntegrationTest;
import com.example.gestionservice.client.WalletClient;
import com.example.gestionservice.client.dto.WalletPaymentRequest;
import com.example.gestionservice.client.dto.WalletPaymentResponse;
import com.example.gestionservice.entity.GestionAccount;
import com.example.gestionservice.entity.ServiceCatalog;
import com.example.gestionservice.entity.ServiceRequest;
import com.example.gestionservice.enums.PaymentStatus;
import com.example.gestionservice.enums.Role;
import com.example.gestionservice.enums.ServiceRequestStatus;
import com.example.gestionservice.repository.GestionAccountRepository;
import com.example.gestionservice.repository.ServiceCatalogRepository;
import com.example.gestionservice.repository.ServiceRequestRepository;
import com.example.gestionservice.support.JwtTestTokenFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'intégration — non-régression du flux de paiement après l'Étape 5.
 *
 * <p>Le cutover {@code telephone → UUID} touche le pipeline de sécurité, mais
 * pas le paiement : le Wallet continue d'identifier le compte bancaire par
 * téléphone. Après le cutover, ce téléphone n'est plus lu dans le JWT, il est
 * relu en base :</p>
 *
 * <pre>
 * JWT sub (UUID) → GestionAccount → principal.getTelephone() → Wallet
 * </pre>
 *
 * <p>Ces tests vérifient que cette chaîne est intacte et que le Wallet reçoit
 * bien le numéro enregistré dans {@code gestion_account}.</p>
 */
@AutoConfigureMockMvc
@DisplayName("Paiement — Non-régression après le cutover UUID (Étape 5)")
class PaymentIdentityIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired GestionAccountRepository gestionAccountRepository;
    @Autowired ServiceCatalogRepository catalogRepository;
    @Autowired ServiceRequestRepository serviceRequestRepository;

    @Value("${jwt.secret}")
    String jwtSecret;

    /** Le Wallet est simulé : aucun appel externe n'est effectué. */
    @MockitoBean WalletClient walletClient;

    private JwtTestTokenFactory tokenFactory;

    private GestionAccount userAccount;
    private String userJwt;
    private String userTelephone;
    private ServiceRequest serviceRequest;

    @BeforeEach
    void setUp() {
        tokenFactory = new JwtTestTokenFactory(jwtSecret);

        userTelephone = JwtTestTokenFactory.uniqueTelephone();
        userAccount = gestionAccountRepository.saveAndFlush(GestionAccount.builder()
                .fullName("Utilisateur Payeur")
                .telephone(userTelephone)
                .email("payeur-" + userTelephone + "@example.com")
                .role(Role.ROLE_USER)
                .active(true)
                .build());

        // Jeton local : sub = gestion_account.id. Aucun téléphone dans le JWT.
        userJwt = tokenFactory.localTokenForAccount(userAccount);

        ServiceCatalog catalog = catalogRepository.saveAndFlush(ServiceCatalog.builder()
                .name("Service payable " + UUID.randomUUID())
                .basePrice(new BigDecimal("250000"))
                .active(true)
                .build());

        serviceRequest = serviceRequestRepository.saveAndFlush(ServiceRequest.builder()
                .accountId(userAccount.getId())
                .serviceCatalog(catalog)
                .title("Demande à payer")
                .description("Test de non-régression du paiement")
                .amount(catalog.getBasePrice())
                .status(ServiceRequestStatus.WAITING_PAYMENT)
                .paymentStatus(PaymentStatus.PENDING)
                .build());
    }

    private void walletAnswersSuccess() {
        when(walletClient.pay(any(WalletPaymentRequest.class))).thenReturn(
                WalletPaymentResponse.builder()
                        .status("SUCCESS")
                        .transactionReference("TXN-CUTOVER-UUID")
                        .build());
    }

    // ==================================================================
    //  Le téléphone du principal alimente toujours le Wallet
    // ==================================================================

    @Test
    @DisplayName("Après authentification par UUID, le Wallet reçoit le téléphone de GestionAccount")
    void payment_withUuidJwt_sendsAccountTelephoneToWallet() throws Exception {
        walletAnswersSuccess();

        mockMvc.perform(post("/api/v1/service-requests/" + serviceRequest.getId() + "/pay")
                        .header("Authorization", "Bearer " + userJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pin", "1234"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        ArgumentCaptor<WalletPaymentRequest> captor = ArgumentCaptor.forClass(WalletPaymentRequest.class);
        verify(walletClient).pay(captor.capture());

        // Le téléphone transmis au Wallet est celui de la base, pas celui du JWT
        // (le JWT ne contient aucun téléphone).
        assertThat(captor.getValue().getTelephone()).isEqualTo(userTelephone);
    }

    @Test
    @DisplayName("Le téléphone du principal n'est pas extrait du claim sub")
    void payment_telephoneNotTakenFromJwtSubject() throws Exception {
        walletAnswersSuccess();

        // Un jeton dont le sub est l'UUID : le téléphone ne peut venir que de la base.
        assertThat(userJwt).isNotBlank();

        mockMvc.perform(post("/api/v1/service-requests/" + serviceRequest.getId() + "/pay")
                        .header("Authorization", "Bearer " + userJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pin", "1234"))))
                .andExpect(status().isOk());

        ArgumentCaptor<WalletPaymentRequest> captor = ArgumentCaptor.forClass(WalletPaymentRequest.class);
        verify(walletClient).pay(captor.capture());

        String sub = io.jsonwebtoken.Jwts.parserBuilder()
                .setSigningKey(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(userJwt)
                .getBody()
                .getSubject();

        // sub = UUID, téléphone = donnée de gestion_account.
        assertThat(sub).isEqualTo(userAccount.getId().toString());
        assertThat(captor.getValue().getTelephone()).isNotEqualTo(sub);
    }

    @Test
    @DisplayName("Paiement réussi → la demande passe à PAID")
    void payment_withUuidJwt_marksRequestAsPaid() throws Exception {
        walletAnswersSuccess();

        mockMvc.perform(post("/api/v1/service-requests/" + serviceRequest.getId() + "/pay")
                        .header("Authorization", "Bearer " + userJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pin", "1234"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attemptStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.data.walletTransactionReference").value("TXN-CUTOVER-UUID"));

        ServiceRequest reloaded = serviceRequestRepository.findById(serviceRequest.getId()).orElseThrow();
        assertThat(reloaded.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    }

    // ==================================================================
    //  Ownership du flux de paiement
    // ==================================================================

    @Test
    @DisplayName("Un compte ne peut pas payer la demande d'un autre compte")
    void payment_otherUserRequest_forbiddenAndWalletNotCalled() throws Exception {
        String otherTelephone = JwtTestTokenFactory.uniqueTelephone();
        GestionAccount other = gestionAccountRepository.saveAndFlush(GestionAccount.builder()
                .fullName("Autre Utilisateur")
                .telephone(otherTelephone)
                .email("autre-" + otherTelephone + "@example.com")
                .role(Role.ROLE_USER)
                .active(true)
                .build());

        mockMvc.perform(post("/api/v1/service-requests/" + serviceRequest.getId() + "/pay")
                        .header("Authorization", "Bearer " + tokenFactory.localTokenForAccount(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pin", "1234"))))
                .andExpect(status().isForbidden());

        verify(walletClient, org.mockito.Mockito.never()).pay(any(WalletPaymentRequest.class));
    }

    @Test
    @DisplayName("Compte désactivé → le paiement est refusé (401) et le Wallet n'est pas appelé")
    void payment_disabledAccount_rejectedAndWalletNotCalled() throws Exception {
        userAccount.setActive(false);
        gestionAccountRepository.saveAndFlush(userAccount);

        mockMvc.perform(post("/api/v1/service-requests/" + serviceRequest.getId() + "/pay")
                        .header("Authorization", "Bearer " + userJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pin", "1234"))))
                .andExpect(status().isUnauthorized());

        verify(walletClient, org.mockito.Mockito.never()).pay(any(WalletPaymentRequest.class));
    }

    @Test
    @DisplayName("Ancien JWT sub=telephone → le paiement est refusé (401)")
    void payment_legacyTelephoneJwt_rejectedAndWalletNotCalled() throws Exception {
        mockMvc.perform(post("/api/v1/service-requests/" + serviceRequest.getId() + "/pay")
                        .header("Authorization", "Bearer " + tokenFactory.legacyTokenForTelephone(userTelephone))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pin", "1234"))))
                .andExpect(status().isUnauthorized());

        verify(walletClient, org.mockito.Mockito.never()).pay(any(WalletPaymentRequest.class));
    }
}
