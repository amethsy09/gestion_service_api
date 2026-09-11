-- ============================================================
-- V4 : Création de la table prestations
-- ============================================================

CREATE TABLE prestations (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    service_request_id  UUID            NOT NULL,
    responsible_id      UUID            NOT NULL,
    name                VARCHAR(255)    NOT NULL,
    description         TEXT,
    status              VARCHAR(50)     NOT NULL DEFAULT 'WAITING_PAYMENT',
    start_date          DATE,
    estimated_end_date  DATE,
    actual_end_date     DATE,
    created_at          TIMESTAMP       NOT NULL,
    updated_at          TIMESTAMP       NOT NULL,

    CONSTRAINT pk_prestations PRIMARY KEY (id),
    CONSTRAINT uk_prestations_service_request UNIQUE (service_request_id),
    CONSTRAINT fk_prestations_service_request
        FOREIGN KEY (service_request_id) REFERENCES service_requests (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_prestations_responsible
        FOREIGN KEY (responsible_id) REFERENCES responsibles (id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_prestations_status
        CHECK (status IN ('DRAFT','WAITING_PAYMENT','PAID','PLANNED','IN_PROGRESS','COMPLETED','CANCELLED')),
    CONSTRAINT chk_prestations_dates
        CHECK (estimated_end_date IS NULL OR start_date IS NULL OR estimated_end_date >= start_date)
);

CREATE INDEX idx_prestations_status         ON prestations (status);
CREATE INDEX idx_prestations_responsible    ON prestations (responsible_id);
CREATE INDEX idx_prestations_service_req    ON prestations (service_request_id);
