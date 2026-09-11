-- ============================================================
-- V3 : Création de la table responsibles
-- ============================================================

CREATE TABLE responsibles (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    first_name      VARCHAR(100)    NOT NULL,
    last_name       VARCHAR(100)    NOT NULL,
    email           VARCHAR(255)    NOT NULL,
    phone_number    VARCHAR(20),
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL,
    updated_at      TIMESTAMP       NOT NULL,

    CONSTRAINT pk_responsibles     PRIMARY KEY (id),
    CONSTRAINT uk_responsibles_email UNIQUE (email)
);

CREATE INDEX idx_responsibles_active ON responsibles (active);
