-- ============================================================
-- V8 : Création de la table tasks
-- ============================================================

CREATE TABLE tasks (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    prestation_id   UUID        NOT NULL,
    resource_id     UUID,
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    status          VARCHAR(50)  NOT NULL DEFAULT 'TODO',
    priority        VARCHAR(50)  NOT NULL DEFAULT 'MEDIUM',
    start_date      DATE,
    due_date        DATE,
    completed_at    TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP    NOT NULL,

    CONSTRAINT pk_tasks PRIMARY KEY (id),
    CONSTRAINT fk_tasks_prestation
        FOREIGN KEY (prestation_id) REFERENCES prestations (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_tasks_resource
        FOREIGN KEY (resource_id) REFERENCES resources (id)
        ON DELETE SET NULL,
    CONSTRAINT chk_tasks_status
        CHECK (status IN ('TODO','IN_PROGRESS','BLOCKED','DONE','CANCELLED')),
    CONSTRAINT chk_tasks_priority
        CHECK (priority IN ('LOW','MEDIUM','HIGH','URGENT')),
    CONSTRAINT chk_tasks_dates
        CHECK (due_date IS NULL OR start_date IS NULL OR due_date >= start_date)
);

CREATE INDEX idx_tasks_prestation_id ON tasks (prestation_id);
CREATE INDEX idx_tasks_resource_id   ON tasks (resource_id);
CREATE INDEX idx_tasks_status        ON tasks (status);
CREATE INDEX idx_tasks_priority      ON tasks (priority);
CREATE INDEX idx_tasks_due_date      ON tasks (due_date);
