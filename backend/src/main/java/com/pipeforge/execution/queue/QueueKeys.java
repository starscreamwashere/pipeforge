package com.pipeforge.execution.queue;

/** Redis key names for the runtime queues (Technical Requirements §7). */
public final class QueueKeys {

    /** Redis List of task-run IDs ready for immediate execution (producer LPUSH, worker BRPOP). */
    public static final String READY_QUEUE = "queue:ready";

    /** Redis Sorted Set of task-run IDs scheduled for delayed retry (Milestone 6). */
    public static final String RETRY_QUEUE = "queue:retry";

    private QueueKeys() {
    }
}
