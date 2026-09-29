package com.example.gestionservice.security;

import com.example.gestionservice.entity.GestionAccount;
import com.example.gestionservice.enums.Role;
import com.example.gestionservice.exception.UnauthorizedException;
import com.example.gestionservice.repository.GestionAccountRepository;
import com.example.gestionservice.support.JwtTestTokenFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests du {@code AccountSecurityResolver} de production — <b>Étape 5</b>.
 *
 * <p>Le resolver résout désormais l'identité par <b>UUID</b>
 * ({@code claims.sub = gestion_account.id}) et non plus par téléphone.
 * Il est devenu strictement lecteur : plus aucune auto-création de compte.</p>
 *
 * <p>Seule la génération du téléphone est centralisée, via
 * {@link JwtTestTokenFactory#uniqueTelephone()}, afin de garantir l'unicité
 * vis-à-vis de la contrainte UNIQUE de {@code gestion_account.telephone}.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccountSecurityResolver — Tests unitaires (résolution par UUID)")
class AccountSecurityResolverTest {

    @Mock GestionAccountRepository gestionAccountRepository;
    @InjectMocks AccountSecurityResolver resolver;

    private UUID accountId;
    private String telephone;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        telephone = JwtTestTokenFactory.uniqueTelephone();
    }

    private GestionAccount accountWith(Role role, boolean active) {
        return GestionAccount.builder()
                .id(accountId)
                .fullName("Test User")
                .telephone(telephone)
                .email("test-" + telephone + "@example.com")
                .role(role)
                .active(active)
                .build();
    }

    // ================================================================
    //  Résolution par UUID — compte existant et actif
    // ================================================================

    @Test
    @DisplayName("UUID connu + compte actif → résolution OK, rôle USER")
    void resolve_knownAccountId_activeAccount_returnsIdentity() {
        GestionAccount account = accountWith(Role.ROLE_USER, true);

        when(gestionAccountRepository.findById(accountId)).thenReturn(Optional.of(account));

        AccountSecurityInfo info = resolver.resolve(accountId);

        assertThat(info.accountId()).isEqualTo(accountId);
        // Le téléphone vient de la base, plus du JWT.
        assertThat(info.telephone()).isEqualTo(telephone);
        assertThat(info.roles()).containsExactly(Role.ROLE_USER);
    }

    @Test
    @DisplayName("UUID connu + compte ADMIN → rôle ADMIN")
    void resolve_knownAccountId_adminAccount_returnsAdminRole() {
        GestionAccount account = accountWith(Role.ROLE_ADMIN, true);

        when(gestionAccountRepository.findById(accountId)).thenReturn(Optional.of(account));

        AccountSecurityInfo info = resolver.resolve(accountId);

        assertThat(info.roles()).containsExactly(Role.ROLE_ADMIN);
        assertThat(info.telephone()).isEqualTo(telephone);
    }

    @Test
    @DisplayName("UUID connu + compte RESPONSIBLE → rôle RESPONSIBLE")
    void resolve_knownAccountId_responsibleAccount_returnsResponsibleRole() {
        GestionAccount account = accountWith(Role.ROLE_RESPONSIBLE, true);

        when(gestionAccountRepository.findById(accountId)).thenReturn(Optional.of(account));

        AccountSecurityInfo info = resolver.resolve(accountId);

        assertThat(info.roles()).containsExactly(Role.ROLE_RESPONSIBLE);
    }

    /**
     * Le téléphone retourné est celui de la base et ne peut pas provenir
     * du JWT : le resolver ne reçoit qu'un UUID, aucune chaîne de téléphone.
     */
    @Test
    @DisplayName("Le téléphone retourné provient de GestionAccount, pas du claim sub")
    void resolve_returnsTelephoneFromAccountNotFromToken() {
        GestionAccount account = accountWith(Role.ROLE_USER, true);

        when(gestionAccountRepository.findById(accountId)).thenReturn(Optional.of(account));

        // L'entrée est un UUID : aucun téléphone n'est fourni au resolver.
        assertThat(resolver.resolve(accountId).telephone()).isEqualTo(account.getTelephone());
    }

    // ================================================================
    //  Aucune auto-création (comportement supprimé à l'Étape 5)
    // ================================================================

    @Test
    @DisplayName("UUID inconnu → UnauthorizedException")
    void resolve_unknownAccountId_throwsUnauthorized() {
        when(gestionAccountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolve(accountId))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("introuvable");
    }

    @Test
    @DisplayName("UUID inconnu → AUCUNE création de compte (verify(save) jamais appelé)")
    void resolve_unknownAccountId_neverCreatesAccount() {
        when(gestionAccountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolve(accountId))
                .isInstanceOf(UnauthorizedException.class);

        // Le point clé du cutover : plus aucune écriture en base.
        verify(gestionAccountRepository).findById(accountId);
        verify(gestionAccountRepository, never()).save(any(GestionAccount.class));
        verify(gestionAccountRepository, never()).saveAndFlush(any(GestionAccount.class));
        verifyNoMoreInteractions(gestionAccountRepository);
    }

    @Test
    @DisplayName("UUID inconnu → la résolution se fait par id, jamais par téléphone")
    void resolve_neverQueriesByTelephone() {
        when(gestionAccountRepository.findById(accountId)).thenReturn(Optional.of(accountWith(Role.ROLE_USER, true)));

        resolver.resolve(accountId);

        verify(gestionAccountRepository, never()).findByTelephone(anyString());
    }

    // ================================================================
    //  Compte désactivé
    // ================================================================

    @Test
    @DisplayName("Compte désactivé → UnauthorizedException")
    void resolve_disabledAccount_throwsUnauthorized() {
        when(gestionAccountRepository.findById(accountId))
                .thenReturn(Optional.of(accountWith(Role.ROLE_USER, false)));

        assertThatThrownBy(() -> resolver.resolve(accountId))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("désactivé");
    }

    @Test
    @DisplayName("Compte désactivé → aucune écriture en base")
    void resolve_disabledAccount_neverWritesToDatabase() {
        when(gestionAccountRepository.findById(accountId))
                .thenReturn(Optional.of(accountWith(Role.ROLE_USER, false)));

        assertThatThrownBy(() -> resolver.resolve(accountId))
                .isInstanceOf(UnauthorizedException.class);

        verify(gestionAccountRepository, never()).save(any(GestionAccount.class));
    }

    // ================================================================
    //  accountId nul
    // ================================================================

    @Test
    @DisplayName("accountId null → UnauthorizedException, sans accès à la base")
    void resolve_nullAccountId_throwsUnauthorized() {
        assertThatThrownBy(() -> resolver.resolve(null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("absent du JWT");

        verifyNoInteractions(gestionAccountRepository);
    }

    // ================================================================
    //  Contrat de résolution
    // ================================================================

    /**
     * Le resolver ne connaît qu'un seul rôle par compte
     * ({@code List.of(role)}). Un ADMIN n'hérite donc pas de ROLE_USER.
     *
     * <p>Ce test fige le comportement : il explique pourquoi
     * {@code POST /api/v1/service-requests}, protégé par
     * {@code hasRole('USER')}, renvoie 403 à un compte ADMIN.</p>
     */
    @Test
    @DisplayName("Un compte ADMIN n'hérite pas du rôle USER (rôle unique par compte)")
    void resolve_adminAccount_doesNotInheritUserRole() {
        when(gestionAccountRepository.findById(accountId))
                .thenReturn(Optional.of(accountWith(Role.ROLE_ADMIN, true)));

        AccountSecurityInfo info = resolver.resolve(accountId);

        assertThat(info.roles()).containsExactly(Role.ROLE_ADMIN);
        assertThat(info.roles()).doesNotContain(Role.ROLE_USER);
    }

    /**
     * Le rôle provient de la base : un rôle en dur dans le resolver
     * (par exemple toujours {@code ROLE_USER}) est ici exclus.
     */
    @Test
    @DisplayName("Le rôle est relu en base à chaque résolution (pas de valeur par défaut codée en dur)")
    void resolve_roleComesFromDatabase() {
        when(gestionAccountRepository.findById(accountId))
                .thenReturn(Optional.of(accountWith(Role.ROLE_ADMIN, true)));

        assertThat(resolver.resolve(accountId).roles())
                .containsExactly(Role.ROLE_ADMIN)
                .doesNotContain(Role.ROLE_USER);
    }
}
