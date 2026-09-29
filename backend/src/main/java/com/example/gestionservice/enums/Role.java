package com.example.gestionservice.enums;

/**
 * Rôles applicatifs gérés localement par gestion-service.
 *
 * <p>Les rôles ne proviennent PAS du JWT (auth_api ne les contient pas).
 * Ils sont stockés dans la table {@code gestion_account} et résolus
 * par {@link com.example.gestionservice.security.AccountSecurityResolver}.</p>
 *
 * <ul>
 *   <li>{@code ROLE_USER} — créer et payer ses demandes</li>
 *   <li>{@code ROLE_ADMIN} — gérer le catalogue, responsables, ressources, spécialités, voir toutes les demandes</li>
 *   <li>{@code ROLE_RESPONSIBLE} — gérer prestations, affecter ressources, créer et suivre les tâches</li>
 * </ul>
 */
public enum Role {
    ROLE_USER,
    ROLE_ADMIN,
    ROLE_RESPONSIBLE
}
