-- Milestone 5.1 — Execution domain (Backend Schema §5: pipeline_runs, task_runs).

CREATE TABLE pipeline_runs (
    id            UUID        PRIMARY KEY,
    pipeline_id   UUID        NOT NULL,
    status        VARCHAR(20) NOT NULL,
    trigger_type  VARCHAR(20) NOT NULL,
    started_at    TIMESTAMPTZ,
    completed_at  TIMESTAMPTZ,
    error_message TEXT,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_pipeline_runs_pipeline FOREIGN KEY (pipeline_id) REFERENCES pipelines (id),
    CONSTRAINT chk_pipeline_runs_status CHECK (
        status IN ('PENDING', 'QUEUED', 'RUNNING', 'SUCCESS', 'FAILED', 'CANCELLED', 'RETRYING')),
    CONSTRAINT chk_pipeline_runs_trigger CHECK (trigger_type IN ('MANUAL', 'CRON', 'API'))
);

CREATE INDEX ix_pipeline_runs_pipeline_id ON pipeline_runs (pipeline_id);
CREATE INDEX ix_pipeline_runs_status ON pipeline_runs (status);

CREATE TABLE task_runs (
    id              UUID        PRIMARY KEY,
    pipeline_run_id UUID        NOT NULL,
    task_id         UUID        NOT NULL,
    status          VARCHAR(20) NOT NULL,
    attempt         INTEGER     NOT NULL DEFAULT 0,
    worker_id       VARCHAR(100),
    started_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    error_message   TEXT,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_task_runs_run FOREIGN KEY (pipeline_run_id) REFERENCES pipeline_runs (id) ON DELETE CASCADE,
    CONSTRAINT fk_task_runs_task FOREIGN KEY (task_id) REFERENCES pipeline_tasks (id),
    CONSTRAINT chk_task_runs_status CHECK (
        status IN ('PENDING', 'QUEUED', 'RUNNING', 'SUCCESS', 'FAILED', 'CANCELLED', 'RETRYING'))
);

CREATE INDEX ix_task_runs_run_id ON task_runs (pipeline_run_id);
CREATE INDEX ix_task_runs_task_id ON task_runs (task_id);
CREATE INDEX ix_task_runs_status ON task_runs (status);
