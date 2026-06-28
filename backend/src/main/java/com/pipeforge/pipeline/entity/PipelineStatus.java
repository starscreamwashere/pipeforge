package com.pipeforge.pipeline.entity;

/** Lifecycle status of a pipeline (Backend Schema §5). */
public enum PipelineStatus {
    DRAFT,
    ACTIVE,
    PAUSED,
    ARCHIVED
}
