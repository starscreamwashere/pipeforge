package com.pipeforge.execution.entity;

/**
 * Lifecycle status shared by pipeline runs and task runs
 * (App Flow §5.5, Technical Requirements state machine §5.5).
 */
public enum ExecutionStatus {
    PENDING,
    QUEUED,
    RUNNING,
    SUCCESS,
    FAILED,
    CANCELLED,
    RETRYING;

    public boolean isTerminal() {
        return this == SUCCESS || this == CANCELLED;
    }
}
