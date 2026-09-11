-- ============================================================
-- V7 : Création de la table prestation_resources
-- ============================================================

CREATE TABLE prestation_resources (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    prestation_id   UUID        NOT NULL,
    resource_id     UUID        NOT NULL,
    assigned_at     TIMESTAMP   NOT NULL DEFAULT NOW(),
    unassigned_at   TIMESTAMP,
    status          VARCHAR(50) NOT NULL DEFAULT 'ASSIGNED',
    created_at      TIMESTAMP   NOT NULL,
    updated_at      TIMESTAMP   NOT NULL,

    CONSTRAINT pk_prestation_resources PRIMARY KEY (id),
    CONSTRAINT uk_prestation_resource UNIQUE (prestation_id, resource_id),
    CONSTRAINT fk_prestation_resources_prestation
        FOREIGN KEY (prestation_id) REFERENCES prestations (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_prestation_resources_resource
        FOREIGN KEY (resource_id) REFERENCES resources (id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_prestation_resources_status
        CHECK (status IN ('ASSIGNED','ACTIVE','COMPLETED','REMOVED'))
);

CREATE INDEX idx_prestation_resources_prestation ON prestation_resources (prestation_id);
CREATE INDEX idx_prestation_resources_resource   ON prestation_resources (resource_id);
CREATE INDEX idx_prestation_resources_status     ON prestation_resources (status);
