package com.pipeforge.pipeline.entity;

/** Type of work a task performs (Backend Schema §5 — {@code pipeline_tasks}). */
public enum TaskType {
    EXTRACT,
    TRANSFORM,
    LOAD,
    CUSTOM
}
