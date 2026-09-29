package com.example.gestionservice.security;

import com.example.gestionservice.enums.Role;

import java.util.List;
import java.util.UUID;

/**
 * Résultat de la résolution d'identité depuis l'UUID du JWT local.
 *
 * @param accountId UUID interne de gestion-service ({@code gestion_account.id},
 *                 égal au claim {@code sub} du jeton local)
 * @param roles     rôles métier locaux (ADMIN / RESPONSIBLE / USER), relus en base
 * @param telephone téléphone du compte, issu de {@code gestion_account.telephone}
 *                 — plus du claim {@code sub} du JWT
 */
public record AccountSecurityInfo(
        UUID accountId,
        List<Role> roles,
        String telephone
) {}
