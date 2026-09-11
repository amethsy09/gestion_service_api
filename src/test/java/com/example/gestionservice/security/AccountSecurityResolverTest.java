package com.example.gestionservice.security;

import com.example.gestionservice.entity.GestionAccount;
import com.example.gestionservice.enums.Role;
import com.example.gestionservice.exception.UnauthorizedException;
import com.example.gestionservice.repository.GestionAccountRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountSecurityResolver — Tests unitaires")
class AccountSecurityResolverTest {

    @Mock GestionAccountRepository gestionAccountRepository;
    @InjectMocks AccountSecurityResolver resolver;

    private String telephone;

    @BeforeEach
    void setUp() {
        telephone = "771234567";
    }

    // ================================================================
    //  Téléphone connu + compte actif
    // ================================================================

    @Test
    @DisplayName("Telephone connu + actif → résolution OK, rôle USER")
    void resolve_knownTelephone_activeAccount_returnsIdentity() {
        GestionAccount account = GestionAccount.builder()
                .id(UUID.randomUUID())
                .telephone(telephone)
                .role(Role.ROLE_USER)
                .active(true)
                .build();

        when(gestionAccountRepository.findByTelephone(telephone)).thenReturn(Optional.of(account));

        AccountSecurityInfo info = resolver.resolve(telephone);

        assertThat(info.accountId()).isEqualTo(account.getId());
        assertThat(info.telephone()).isEqualTo(telephone);
        assertThat(info.roles()).containsExactly(Role.ROLE_USER);
    }

    @Test
    @DisplayName("Telephone connu + compte ADMIN → rôle ADMIN")
    void resolve_knownTelephone_adminAccount_returnsAdminRole() {
        GestionAccount account = GestionAccount.builder()
                .id(UUID.randomUUID())
                .telephone(telephone)
                .role(Role.ROLE_ADMIN)
                .active(true)
                .build();

        when(gestionAccountRepository.findByTelephone(telephone)).thenReturn(Optional.of(account));

        AccountSecurityInfo info = resolver.resolve(telephone);

        assertThat(info.roles()).containsExactly(Role.ROLE_ADMIN);
    }

    @Test
    @DisplayName("Telephone connu + compte RESPONSIBLE → rôle RESPONSIBLE")
    void resolve_knownTelephone_responsibleAccount_returnsResponsibleRole() {
        GestionAccount account = GestionAccount.builder()
                .id(UUID.randomUUID())
                .telephone(telephone)
                .role(Role.ROLE_RESPONSIBLE)
                .active(true)
                .build();

        when(gestionAccountRepository.findByTelephone(telephone)).thenReturn(Optional.of(account));

        AccountSecurityInfo info = resolver.resolve(telephone);

        assertThat(info.roles()).containsExactly(Role.ROLE_RESPONSIBLE);
    }

    // ================================================================
    //  Téléphone inconnu → création automatique ROLE_USER
    // ================================================================

    @Test
    @DisplayName("Telephone inconnu → création gestion_account avec ROLE_USER")
    void resolve_unknownTelephone_createsAccountWithUserRole() {
        when(gestionAccountRepository.findByTelephone(telephone)).thenReturn(Optional.empty());

        GestionAccount savedAccount = GestionAccount.builder()
                .id(UUID.randomUUID())
                .telephone(telephone)
                .role(Role.ROLE_USER)
                .active(true)
                .build();

        when(gestionAccountRepository.save(any(GestionAccount.class))).thenReturn(savedAccount);

        AccountSecurityInfo info = resolver.resolve(telephone);

        assertThat(info.telephone()).isEqualTo(telephone);
        assertThat(info.roles()).containsExactly(Role.ROLE_USER);
        verify(gestionAccountRepository).save(any(GestionAccount.class));
    }

    // ================================================================
    //  Compte désactivé → accès refusé
    // ================================================================

    @Test
    @DisplayName("Compte désactivé → UnauthorizedException")
    void resolve_disabledAccount_throwsUnauthorized() {
        GestionAccount account = GestionAccount.builder()
                .id(UUID.randomUUID())
                .telephone(telephone)
                .role(Role.ROLE_USER)
                .active(false)
                .build();

        when(gestionAccountRepository.findByTelephone(telephone)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> resolver.resolve(telephone))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("désactivé");
    }

    // ================================================================
    //  Téléphone null ou vide
    // ================================================================

    @Test
    @DisplayName("Telephone null → UnauthorizedException")
    void resolve_nullTelephone_throwsUnauthorized() {
        assertThatThrownBy(() -> resolver.resolve(null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Téléphone");
    }

    @Test
    @DisplayName("Telephone vide → UnauthorizedException")
    void resolve_emptyTelephone_throwsUnauthorized() {
        assertThatThrownBy(() -> resolver.resolve(" "))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Téléphone");
    }
}
