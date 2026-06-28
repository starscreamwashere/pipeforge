package com.pipeforge.pipeline.entity;

/**
 * Retry configuration stored as JSONB on a pipeline (PRD §5.7, Backend Schema §5).
 *
 * <p>Retries use exponential backoff: {@code delay = backoffSeconds * 2^attempt},
 * giving up to {@code maxRetries} attempts before the dead-letter state.
 */
public record RetryPolicy(
        int maxRetries,
        long backoffSeconds) {

    public static RetryPolicy defaults() {
        return new RetryPolicy(3, 5);
    }
}
