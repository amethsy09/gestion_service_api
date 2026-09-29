-- ============================================================
-- V1 : Création de la table service_catalog
-- ============================================================

CREATE TABLE service_catalog (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    name            VARCHAR(255)    NOT NULL,
    description     TEXT,
    base_price      NUMERIC(15, 2)  NOT NULL,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL,
    updated_at      TIMESTAMP       NOT NULL,

    CONSTRAINT pk_service_catalog PRIMARY KEY (id),
    CONSTRAINT uk_service_catalog_name UNIQUE (name),
    CONSTRAINT chk_service_catalog_base_price CHECK (base_price > 0)
);

CREATE INDEX idx_service_catalog_active ON service_catalog (active);

-- Données initiales : exemples de services
INSERT INTO service_catalog (id, name, description, base_price, active, created_at, updated_at) VALUES
    (gen_random_uuid(), 'Développement application mobile',  'Conception et développement d''une application mobile iOS/Android', 2500000.00, TRUE, NOW(), NOW()),
    (gen_random_uuid(), 'Création site web',                 'Conception et développement d''un site web vitrine ou e-commerce',  1500000.00, TRUE, NOW(), NOW()),
    (gen_random_uuid(), 'Développement application web',     'Conception et développement d''une application web SPA ou MPA',    2000000.00, TRUE, NOW(), NOW()),
    (gen_random_uuid(), 'Développement API',                 'Conception et développement d''une API REST ou GraphQL',            1000000.00, TRUE, NOW(), NOW()),
    (gen_random_uuid(), 'Maintenance informatique',          'Maintenance corrective et évolutive de systèmes existants',          500000.00, TRUE, NOW(), NOW()),
    (gen_random_uuid(), 'Consulting informatique',           'Accompagnement et conseil en transformation numérique',              750000.00, TRUE, NOW(), NOW());
