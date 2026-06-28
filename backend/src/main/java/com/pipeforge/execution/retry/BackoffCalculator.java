package com.pipeforge.execution.retry;

import org.springframework.stereotype.Component;

/**
 * Exponential backoff for retries (PRD §5.7, Doc 6 §6.5):
 * {@code delay = base * 2^attempt}, capped to avoid overflow / runaway delays.
 */
@Component
public class BackoffCalculator {

    private static final int MAX_EXPONENT = 16;

    public long computeDelaySeconds(int attempt, long baseSeconds) {
        int exponent = Math.min(Math.max(attempt, 0), MAX_EXPONENT);
        return baseSeconds * (1L << exponent);
    }
}
