package com.example.gestionservice.security;

import com.example.gestionservice.enums.Role;

import java.util.List;
import java.util.UUID;

/**
 * Résultat de la résolution d'identité depuis le téléphone du JWT.
 *
 * @param accountId  UUID interne de gestion-service (provenant de {@code gestion_account.id})
 * @param roles      rôles métier locaux (ADMIN / RESPONSIBLE / USER)
 * @param telephone  téléphone extrait du JWT {@code sub}
 */
public record AccountSecurityInfo(
        UUID accountId,
        List<Role> roles,
        String telephone
) {}
