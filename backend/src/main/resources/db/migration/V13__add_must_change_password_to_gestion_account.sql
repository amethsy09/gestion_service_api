-- ============================================================
-- V13 : Ajout du champ must_change_password à gestion_account
-- Permet d'obliger un changement de mot de passe à la prochaine connexion.
-- Utilisé pour les comptes créés avec un mot de passe temporaire (admin -> responsable).
-- ============================================================

ALTER TABLE gestion_account
    ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;