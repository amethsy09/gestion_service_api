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
import java.util.UUID;

/**
 * Résout l'identité gestion-service à partir de l'UUID porté par le JWT local.
 *
 * <p>Flux (Étape 5 — cutover définitif {@code telephone → UUID}) :<pre>
 * accountId (claim sub du JWT local)
 *      ↓
 * gestion_account (recherche par id)
 *      ↓
 * UUID interne + rôles + téléphone
 * </pre>
 *
 * <p>Comportement :<ul>
 *   <li>Compte existant + {@code active = true} → authentification autorisée</li>
 *   <li>Compte inexistant → {@link UnauthorizedException}, <b>aucune création</b></li>
 *   <li>Compte désactivé → {@link UnauthorizedException}</li>
 *   <li>Rôle relu en base à chaque requête ; {@code ROLE_USER} seulement si la
 *       colonne est nulle. Jamais de valeur de rôle provenant du JWT.</li>
 * </ul></p>
 *
 * <p><b>Plus aucune auto-création de compte.</b> Avant l'Étape 5, un téléphone
 * inconnu créait automatiquement un {@code gestion_account} en
 * {@code ROLE_USER}. Ce comportement est définitivement supprimé : un compte
 * existe soit parce qu'il a été inscrit via {@code /api/v1/auth/register},
 * soit parce qu'il a été créé par le seeder ADMIN, soit parce qu'un
 * administrateur l'a créé. Rien d'autre.</p>
 *
 * <p>La méthode est en lecture seule : elle ne modifie jamais la base.</p>
 *
 * <p>Le téléphone n'est plus une entrée de résolution mais reste restitué,
 * car il provient désormais de {@code gestion_account.telephone} et sert
 * toujours au flux de paiement (appel au Wallet).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountSecurityResolver {

    private final GestionAccountRepository gestionAccountRepository;

    /**
     * Résout un compte à partir de son UUID interne.
     *
     * @param accountId UUID issu du claim {@code sub} du JWT local
     * @return l'identité de sécurité (UUID, rôles, téléphone)
     * @throws UnauthorizedException si l'UUID est nul, inconnu, ou le compte désactivé
     */
    @Transactional(readOnly = true)
    public AccountSecurityInfo resolve(UUID accountId) {
        if (accountId == null) {
            throw new UnauthorizedException("Identifiant de compte absent du JWT (claim sub)");
        }

        GestionAccount account = gestionAccountRepository.findById(accountId)
                .orElseThrow(() -> {
                    log.warn("Compte gestion introuvable pour accountId={} — aucun compte créé", accountId);
                    return new UnauthorizedException("Compte gestion introuvable pour accountId=" + accountId);
                });

        if (!Boolean.TRUE.equals(account.getActive())) {
            log.warn("Compte gestion désactivé pour accountId={}", accountId);
            throw new UnauthorizedException("Compte gestion désactivé pour accountId=" + accountId);
        }

        Role role = account.getRole() != null ? account.getRole() : Role.ROLE_USER;

        // Le téléphone provient de la base, plus du JWT.
        return new AccountSecurityInfo(account.getId(), List.of(role), account.getTelephone());
    }
}
