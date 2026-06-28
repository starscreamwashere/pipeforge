package com.pipeforge.execution.entity;

/** How an execution was started (App Flow §5.5). */
public enum TriggerType {
    MANUAL,
    CRON,
    API
}
