-- ============================================================
-- V9 : Création de la table payment_attempts
-- ============================================================

CREATE TABLE payment_attempts (
    id                              UUID            NOT NULL DEFAULT gen_random_uuid(),
    service_request_id              UUID            NOT NULL,
    account_id                      UUID            NOT NULL,
    amount                          NUMERIC(15, 2)  NOT NULL,
    status                          VARCHAR(50)     NOT NULL DEFAULT 'PENDING',
    wallet_transaction_reference    VARCHAR(255),
    idempotency_key                 UUID            NOT NULL,
    failure_reason                  TEXT,
    attempt_number                  INTEGER         NOT NULL DEFAULT 1,
    created_at                      TIMESTAMP       NOT NULL,
    updated_at                      TIMESTAMP       NOT NULL,

    CONSTRAINT pk_payment_attempts PRIMARY KEY (id),
    CONSTRAINT uk_payment_attempts_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT fk_payment_attempts_service_request
        FOREIGN KEY (service_request_id) REFERENCES service_requests (id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_payment_attempts_status
        CHECK (status IN ('PENDING','PROCESSING','SUCCESS','FAILED')),
    CONSTRAINT chk_payment_attempts_attempt_number
        CHECK (attempt_number >= 1)
);

CREATE INDEX idx_payment_attempts_service_request  ON payment_attempts (service_request_id);
CREATE INDEX idx_payment_attempts_account_id       ON payment_attempts (account_id);
CREATE INDEX idx_payment_attempts_idempotency_key  ON payment_attempts (idempotency_key);
CREATE INDEX idx_payment_attempts_status           ON payment_attempts (status);
