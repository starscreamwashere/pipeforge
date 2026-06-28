-- Milestone 7.1 — Worker registry (Backend Schema §5, App Flow §5.9).

CREATE TABLE workers (
    id                  UUID        PRIMARY KEY,
    name                VARCHAR(150) NOT NULL,
    status              VARCHAR(20) NOT NULL,
    jobs_processed      BIGINT      NOT NULL DEFAULT 0,
    current_task_run_id UUID,
    last_heartbeat_at   TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_workers_status CHECK (status IN ('ACTIVE', 'BUSY', 'OFFLINE'))
);

CREATE UNIQUE INDEX ux_workers_name ON workers (name);
CREATE INDEX ix_workers_status ON workers (status);
