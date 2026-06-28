package com.pipeforge.execution.retry;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Exponential backoff timings (Phase 6.5 — "retry timings correct"). */
class BackoffCalculatorTest {

    private final BackoffCalculator calculator = new BackoffCalculator();

    @Test
    void doublesDelayEachAttempt() {
        assertThat(calculator.computeDelaySeconds(0, 5)).isEqualTo(5);   // 5 * 2^0
        assertThat(calculator.computeDelaySeconds(1, 5)).isEqualTo(10);  // 5 * 2^1
        assertThat(calculator.computeDelaySeconds(2, 5)).isEqualTo(20);  // 5 * 2^2
        assertThat(calculator.computeDelaySeconds(3, 5)).isEqualTo(40);  // 5 * 2^3
    }

    @Test
    void capsTheExponentToAvoidOverflow() {
        long capped = calculator.computeDelaySeconds(1000, 1);
        assertThat(capped).isEqualTo(1L << 16);
    }
}
