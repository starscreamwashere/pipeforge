-- Milestone 6.3 — Retry tracking + dead-letter status.

CREATE TABLE retries (
    id           UUID        PRIMARY KEY,
    task_run_id  UUID        NOT NULL,
    attempt      INTEGER     NOT NULL,
    scheduled_at TIMESTAMPTZ NOT NULL,
    reason       TEXT,
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_retries_task_run FOREIGN KEY (task_run_id) REFERENCES task_runs (id) ON DELETE CASCADE
);

CREATE INDEX ix_retries_task_run_id ON retries (task_run_id);

-- Add the FAILED_PERMANENTLY dead-letter status to the execution CHECK constraints.
ALTER TABLE pipeline_runs DROP CONSTRAINT chk_pipeline_runs_status;
ALTER TABLE pipeline_runs ADD CONSTRAINT chk_pipeline_runs_status CHECK (
    status IN ('PENDING', 'QUEUED', 'RUNNING', 'SUCCESS', 'FAILED', 'CANCELLED', 'RETRYING', 'FAILED_PERMANENTLY'));

ALTER TABLE task_runs DROP CONSTRAINT chk_task_runs_status;
ALTER TABLE task_runs ADD CONSTRAINT chk_task_runs_status CHECK (
    status IN ('PENDING', 'QUEUED', 'RUNNING', 'SUCCESS', 'FAILED', 'CANCELLED', 'RETRYING', 'FAILED_PERMANENTLY'));
