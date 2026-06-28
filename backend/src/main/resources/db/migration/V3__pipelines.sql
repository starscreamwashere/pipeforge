-- Milestone 3.1 — Pipeline domain (Backend Schema §5).

CREATE TABLE pipelines (
    id            UUID         PRIMARY KEY,
    name          VARCHAR(150) NOT NULL,
    description   TEXT,
    schedule_cron VARCHAR(100),
    retry_policy  JSONB,
    owner_id      UUID         NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    version       INTEGER      NOT NULL DEFAULT 1,
    is_deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_pipelines_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT chk_pipelines_status CHECK (status IN ('DRAFT', 'ACTIVE', 'PAUSED', 'ARCHIVED'))
);

CREATE INDEX ix_pipelines_owner_id ON pipelines (owner_id);
CREATE INDEX ix_pipelines_status ON pipelines (status);
CREATE INDEX ix_pipelines_schedule_cron ON pipelines (schedule_cron);
