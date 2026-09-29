-- ============================================================
-- V5 : Création de la table specialties
-- ============================================================

CREATE TABLE specialties (
    id          UUID            NOT NULL DEFAULT gen_random_uuid(),
    name        VARCHAR(255)    NOT NULL,
    description TEXT,
    created_at  TIMESTAMP       NOT NULL,
    updated_at  TIMESTAMP       NOT NULL,

    CONSTRAINT pk_specialties     PRIMARY KEY (id),
    CONSTRAINT uk_specialties_name UNIQUE (name)
);

-- Données initiales : spécialités types
INSERT INTO specialties (id, name, description, created_at, updated_at) VALUES
    (gen_random_uuid(), 'Backend Developer',  'Développement de services et APIs côté serveur',     NOW(), NOW()),
    (gen_random_uuid(), 'Frontend Developer', 'Développement d''interfaces utilisateur web',         NOW(), NOW()),
    (gen_random_uuid(), 'Mobile Developer',   'Développement d''applications mobiles iOS/Android',  NOW(), NOW()),
    (gen_random_uuid(), 'UI/UX Designer',     'Conception d''interfaces et expériences utilisateur', NOW(), NOW()),
    (gen_random_uuid(), 'DevOps Engineer',    'Automatisation, déploiement et infrastructure',       NOW(), NOW()),
    (gen_random_uuid(), 'QA Engineer',        'Tests, qualité et assurance qualité logicielle',      NOW(), NOW()),
    (gen_random_uuid(), 'Project Manager',    'Gestion de projet et coordination des équipes',       NOW(), NOW());
