package com.pipeforge.execution.state;

import com.pipeforge.exception.InvalidStateTransitionException;
import com.pipeforge.execution.entity.ExecutionStatus;
import org.junit.jupiter.api.Test;

import static com.pipeforge.execution.entity.ExecutionStatus.CANCELLED;
import static com.pipeforge.execution.entity.ExecutionStatus.FAILED;
import static com.pipeforge.execution.entity.ExecutionStatus.PENDING;
import static com.pipeforge.execution.entity.ExecutionStatus.QUEUED;
import static com.pipeforge.execution.entity.ExecutionStatus.RETRYING;
import static com.pipeforge.execution.entity.ExecutionStatus.RUNNING;
import static com.pipeforge.execution.entity.ExecutionStatus.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** State-machine guard tests (Phase 5.5 — "invalid transitions blocked"). */
class ExecutionStateMachineTest {

    private final ExecutionStateMachine stateMachine = new ExecutionStateMachine();

    @Test
    void allowsTheHappyPath() {
        assertThat(stateMachine.canTransition(PENDING, QUEUED)).isTrue();
        assertThat(stateMachine.canTransition(QUEUED, RUNNING)).isTrue();
        assertThat(stateMachine.canTransition(RUNNING, SUCCESS)).isTrue();
        assertThatCode(() -> stateMachine.assertCanTransition(RUNNING, FAILED)).doesNotThrowAnyException();
    }

    @Test
    void allowsRetryCycle() {
        assertThat(stateMachine.canTransition(RUNNING, RETRYING)).isTrue();
        assertThat(stateMachine.canTransition(RETRYING, QUEUED)).isTrue();
        assertThat(stateMachine.canTransition(FAILED, RETRYING)).isTrue();
    }

    @Test
    void allowsDeadLetterAndTreatsItAsTerminal() {
        assertThat(stateMachine.canTransition(FAILED, ExecutionStatus.FAILED_PERMANENTLY)).isTrue();
        assertThat(ExecutionStatus.FAILED_PERMANENTLY.isTerminal()).isTrue();
        assertThat(stateMachine.canTransition(ExecutionStatus.FAILED_PERMANENTLY, RETRYING)).isFalse();
    }

    @Test
    void blocksIllegalTransitions() {
        assertThat(stateMachine.canTransition(PENDING, RUNNING)).isFalse();   // must be queued first
        assertThat(stateMachine.canTransition(SUCCESS, RUNNING)).isFalse();   // terminal
        assertThat(stateMachine.canTransition(CANCELLED, QUEUED)).isFalse();  // terminal
        assertThat(stateMachine.canTransition(RUNNING, PENDING)).isFalse();   // no going back

        assertThatThrownBy(() -> stateMachine.assertCanTransition(SUCCESS, RUNNING))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void terminalStatesAreTerminal() {
        assertThat(SUCCESS.isTerminal()).isTrue();
        assertThat(CANCELLED.isTerminal()).isTrue();
        assertThat(RUNNING.isTerminal()).isFalse();
    }
}
