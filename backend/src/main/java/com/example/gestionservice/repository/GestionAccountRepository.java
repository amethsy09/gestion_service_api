package com.example.gestionservice.repository;

import com.example.gestionservice.entity.GestionAccount;
import com.example.gestionservice.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository pour l'entité {@code gestion_account}.
 *
 * <p>Le telephone et l'email sont tous deux uniques au niveau PostgreSQL
 * (contraintes V10 et V12). Les méthodes de recherche sont utilisées
 * par le service d'authentification locale.</p>
 */
@Repository
public interface GestionAccountRepository extends JpaRepository<GestionAccount, UUID> {

    Optional<GestionAccount> findByTelephone(String telephone);

    Optional<GestionAccount> findByEmail(String email);

    boolean existsByTelephone(String telephone);

    boolean existsByEmail(String email);

    /**
     * Indique s'il existe au moins un compte portant ce rôle.
     * Utilisé par le seeder ADMIN pour être idempotent : tant qu'un
     * {@code ROLE_ADMIN} existe, aucun compte n'est créé.
     */
    boolean existsByRole(Role role);
}