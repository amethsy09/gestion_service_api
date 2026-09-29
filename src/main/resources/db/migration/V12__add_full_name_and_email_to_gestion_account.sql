-- ============================================================
-- V12 : Ajout de full_name et email à gestion_account
-- Préparation du schéma pour l'authentification locale (Étape 3).
--
-- Contexte :
--   V10 a créé gestion_account avec telephone, wallet_account_id, role, active.
--   V11 a ajouté password (NULLABLE, attendu pour la prochaine étape).
--   Aucune donnée existante n'est présente à ce stade : la table est vide.
--
-- Ce qui est ajouté :
--   full_name  VARCHAR(150) NOT NULL
--   email      VARCHAR(255) NOT NULL UNIQUE
--
-- Contraintes :
--   telephone déjà UNIQUE via V10 (uk_gestion_account_telephone) — inchangé.
--   email      UNIQUE au niveau PostgreSQL (contrainte d'unicité garantie).
--
-- NOT NULL :
--   La table étant vide, les colonnes sont créées NOT NULL directement.
--   Si des données existaient, il faudrait d'abord les compléter, puis
--   poser la contrainte. Ici, V10/V11 viennent d'être appliquées et
--   aucun compte n'existe encore.
--
-- Aucune suppression, aucune recréation : les colonnes existantes
-- (telephone, wallet_account_id, role, active, password) sont inchangées.
-- ============================================================

ALTER TABLE gestion_account
    ADD COLUMN full_name VARCHAR(150) NOT NULL,
    ADD COLUMN email     VARCHAR(255) NOT NULL;

-- Contrainte d'unicité sur email (le téléphone l'est déjà via V10).
ALTER TABLE gestion_account
    ADD CONSTRAINT uk_gestion_account_email UNIQUE (email);

-- Index de recherche courante par email (utile pour login et vérification d'unicité).
CREATE INDEX idx_gestion_account_email ON gestion_account (email);