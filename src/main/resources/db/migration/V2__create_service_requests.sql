-- ============================================================
-- V2 : Création de la table service_requests
-- ============================================================

CREATE TABLE service_requests (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    account_id          UUID            NOT NULL,
    service_catalog_id  UUID            NOT NULL,
    title               VARCHAR(255)    NOT NULL,
    description         TEXT,
    amount              NUMERIC(15, 2)  NOT NULL,
    status              VARCHAR(50)     NOT NULL DEFAULT 'WAITING_PAYMENT',
    payment_status      VARCHAR(50)     NOT NULL DEFAULT 'PENDING',
    created_at          TIMESTAMP       NOT NULL,
    updated_at          TIMESTAMP       NOT NULL,

    CONSTRAINT pk_service_requests PRIMARY KEY (id),
    CONSTRAINT fk_service_requests_catalog
        FOREIGN KEY (service_catalog_id) REFERENCES service_catalog (id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_service_requests_status
        CHECK (status IN ('CREATED','WAITING_PAYMENT','PAID','IN_PROGRESS','COMPLETED','CANCELLED')),
    CONSTRAINT chk_service_requests_payment_status
        CHECK (payment_status IN ('PENDING','PROCESSING','PAID','FAILED','CANCELLED'))
);

CREATE INDEX idx_service_requests_account_id        ON service_requests (account_id);
CREATE INDEX idx_service_requests_status            ON service_requests (status);
CREATE INDEX idx_service_requests_payment_status    ON service_requests (payment_status);
CREATE INDEX idx_service_requests_catalog           ON service_requests (service_catalog_id);
