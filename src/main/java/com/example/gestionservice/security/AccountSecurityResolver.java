package com.example.gestionservice.security;

import com.example.gestionservice.entity.GestionAccount;
import com.example.gestionservice.enums.Role;
import com.example.gestionservice.exception.UnauthorizedException;
import com.example.gestionservice.repository.GestionAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Résout l'identité gestion-service à partir du téléphone contenu dans le JWT.
 *
 * <p>Flux :</p>
 * <pre>
 * telephone (du JWT sub)
 *      ↓
 * gestion_account (recherche par telephone)
 *      ↓
 * UUID interne + rôles
 * </pre>
 *
 * <p>Comportement :</p>
 * <ul>
 *   <li>Téléphone trouvé + compte actif → authentification autorisée</li>
 *   <li>Téléphone inconnu → création automatique du compte avec {@code ROLE_USER}</li>
 *   <li>Compte désactivé → {@link com.example.gestionservice.exception.UnauthorizedException}</li>
 *   <li>Rôle absent → {@code ROLE_USER} par défaut (jamais ADMIN/RESPONSIBLE)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountSecurityResolver {

    private final GestionAccountRepository gestionAccountRepository;

    @Transactional(readOnly = true)
    public AccountSecurityInfo resolve(String telephone) {
        if (telephone == null || telephone.isBlank()) {
            throw new UnauthorizedException("Téléphone absent du JWT (claim sub)");
        }

        GestionAccount account = gestionAccountRepository.findByTelephone(telephone)
                .orElseGet(() -> {
                    log.info("Première authentification — création gestion_account pour telephone={}", telephone);
                    GestionAccount newAccount = GestionAccount.builder()
                            .telephone(telephone)
                            .role(Role.ROLE_USER)
                            .active(true)
                            .build();
                    return gestionAccountRepository.save(newAccount);
                });

        if (!account.getActive()) {
            throw new UnauthorizedException("Compte gestion désactivé pour telephone=" + telephone);
        }

        Role role = account.getRole();
        if (role == null) {
            role = Role.ROLE_USER;
        }
        List<Role> roles = List.of(role);

        return new AccountSecurityInfo(account.getId(), roles, account.getTelephone());
    }
}
