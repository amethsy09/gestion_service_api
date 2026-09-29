-- ============================================================
-- V10 : Création de la table gestion_account
-- Identité locale de gestion-service, résolue à partir du téléphone du JWT.
-- Le JWT (auth_api) ne contient PAS d'accountId ni de roles.
-- ============================================================

CREATE TABLE gestion_account (
    id                UUID         NOT NULL DEFAULT gen_random_uuid(),
    telephone         VARCHAR(20)  NOT NULL,
    wallet_account_id BIGINT,
    role              VARCHAR(30)  NOT NULL DEFAULT 'ROLE_USER',
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP    NOT NULL,
    updated_at        TIMESTAMP    NOT NULL,

    CONSTRAINT pk_gestion_account PRIMARY KEY (id),
    CONSTRAINT uk_gestion_account_telephone UNIQUE (telephone),
    CONSTRAINT chk_gestion_account_role
        CHECK (role IN ('ROLE_USER','ROLE_ADMIN','ROLE_RESPONSIBLE'))
);

CREATE INDEX idx_gestion_account_telephone ON gestion_account (telephone);
CREATE INDEX idx_gestion_account_wallet_id  ON gestion_account (wallet_account_id);
CREATE INDEX idx_gestion_account_active     ON gestion_account (active);
