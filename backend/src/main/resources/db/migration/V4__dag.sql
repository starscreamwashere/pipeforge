-- Milestone 4.1 — DAG domain (Backend Schema §5: pipeline_tasks, pipeline_dependencies).

CREATE TABLE pipeline_tasks (
    id                   UUID         PRIMARY KEY,
    pipeline_id          UUID         NOT NULL,
    task_name            VARCHAR(150) NOT NULL,
    task_type            VARCHAR(20)  NOT NULL,
    config_payload       JSONB,
    timeout_seconds      INTEGER,
    execution_order_hint INTEGER,
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_pipeline_tasks_pipeline FOREIGN KEY (pipeline_id) REFERENCES pipelines (id) ON DELETE CASCADE,
    CONSTRAINT chk_pipeline_tasks_type CHECK (task_type IN ('EXTRACT', 'TRANSFORM', 'LOAD', 'CUSTOM'))
);

CREATE INDEX ix_pipeline_tasks_pipeline_id ON pipeline_tasks (pipeline_id);
CREATE INDEX ix_pipeline_tasks_task_type ON pipeline_tasks (task_type);

-- Each row is a DAG edge: parent_task_id -> child_task_id, i.e. "child depends on parent".
CREATE TABLE pipeline_dependencies (
    id             UUID        PRIMARY KEY,
    pipeline_id    UUID        NOT NULL,
    parent_task_id UUID        NOT NULL,
    child_task_id  UUID        NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_pd_pipeline FOREIGN KEY (pipeline_id) REFERENCES pipelines (id) ON DELETE CASCADE,
    CONSTRAINT fk_pd_parent FOREIGN KEY (parent_task_id) REFERENCES pipeline_tasks (id) ON DELETE CASCADE,
    CONSTRAINT fk_pd_child FOREIGN KEY (child_task_id) REFERENCES pipeline_tasks (id) ON DELETE CASCADE,
    CONSTRAINT chk_pd_no_self_loop CHECK (parent_task_id <> child_task_id),
    CONSTRAINT uq_pd_edge UNIQUE (pipeline_id, parent_task_id, child_task_id)
);

CREATE INDEX ix_pd_pipeline_id ON pipeline_dependencies (pipeline_id);
CREATE INDEX ix_pd_parent ON pipeline_dependencies (parent_task_id);
CREATE INDEX ix_pd_child ON pipeline_dependencies (child_task_id);
