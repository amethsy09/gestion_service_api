-- ============================================================
-- V11 : Ajout du champ password à gestion_account
-- Préparation du schéma pour l'authentification locale.
--
-- COLONNE VOLONTAIREMENT NULLABLE À CETTE ÉTAPE.
-- La contrainte NOT NULL sera posée par une migration ultérieure,
-- une fois register/login en place et qu'aucun compte ne sera resté
-- sans mot de passe.
--
-- L'entité GestionAccount n'est PAS encore modifiée : ddl-auto=validate
-- doit continuer à valider le schéma avec l'entité actuelle. Un champ
-- présent en base mais absent de l'entité est accepté par Hibernate ;
-- l'inverse ne l'est pas.
--
-- Format de stockage attendu : hash BCrypt (60 caractères).
-- VARCHAR(100) laisse la place pour un passage ultérieur à Argon2 (~96).
-- Aucun index n'est créé : une colonne d'authentification n'a pas à être indexée.
-- ============================================================

ALTER TABLE gestion_account
    ADD COLUMN password VARCHAR(100);
