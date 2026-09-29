-- ============================================================
-- V6 : Création de la table resources
-- ============================================================

CREATE TABLE resources (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    first_name          VARCHAR(100)    NOT NULL,
    last_name           VARCHAR(100)    NOT NULL,
    email               VARCHAR(255)    NOT NULL,
    phone_number        VARCHAR(20),
    specialty_id        UUID            NOT NULL,
    availability_status VARCHAR(50)     NOT NULL DEFAULT 'AVAILABLE',
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP       NOT NULL,
    updated_at          TIMESTAMP       NOT NULL,

    CONSTRAINT pk_resources PRIMARY KEY (id),
    CONSTRAINT uk_resources_email UNIQUE (email),
    CONSTRAINT fk_resources_specialty
        FOREIGN KEY (specialty_id) REFERENCES specialties (id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_resources_availability
        CHECK (availability_status IN ('AVAILABLE','BUSY','UNAVAILABLE'))
);

CREATE INDEX idx_resources_specialty        ON resources (specialty_id);
CREATE INDEX idx_resources_availability     ON resources (availability_status);
CREATE INDEX idx_resources_active           ON resources (active);
