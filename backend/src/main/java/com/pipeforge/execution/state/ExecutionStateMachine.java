package com.pipeforge.execution.state;

import com.pipeforge.exception.InvalidStateTransitionException;
import com.pipeforge.execution.entity.ExecutionStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import static com.pipeforge.execution.entity.ExecutionStatus.CANCELLED;
import static com.pipeforge.execution.entity.ExecutionStatus.FAILED;
import static com.pipeforge.execution.entity.ExecutionStatus.FAILED_PERMANENTLY;
import static com.pipeforge.execution.entity.ExecutionStatus.PENDING;
import static com.pipeforge.execution.entity.ExecutionStatus.QUEUED;
import static com.pipeforge.execution.entity.ExecutionStatus.RETRYING;
import static com.pipeforge.execution.entity.ExecutionStatus.RUNNING;
import static com.pipeforge.execution.entity.ExecutionStatus.SUCCESS;

/**
 * Guards execution status transitions (Technical Requirements §5.5).
 *
 * <pre>
 *   PENDING  -> QUEUED, CANCELLED
 *   QUEUED   -> RUNNING, CANCELLED
 *   RUNNING  -> SUCCESS, FAILED, CANCELLED, RETRYING
 *   RETRYING -> QUEUED, CANCELLED
 *   FAILED   -> RETRYING            (manual / automatic retry, Milestone 6)
 *   SUCCESS / CANCELLED             (terminal)
 * </pre>
 */
@Component
public class ExecutionStateMachine {

    private static final Map<ExecutionStatus, Set<ExecutionStatus>> ALLOWED =
            new EnumMap<>(ExecutionStatus.class);

    static {
        ALLOWED.put(PENDING, Set.of(QUEUED, CANCELLED));
        ALLOWED.put(QUEUED, Set.of(RUNNING, CANCELLED));
        ALLOWED.put(RUNNING, Set.of(SUCCESS, FAILED, CANCELLED, RETRYING));
        ALLOWED.put(RETRYING, Set.of(QUEUED, CANCELLED));
        ALLOWED.put(FAILED, Set.of(RETRYING, FAILED_PERMANENTLY));
        ALLOWED.put(SUCCESS, Set.of());
        ALLOWED.put(CANCELLED, Set.of());
        ALLOWED.put(FAILED_PERMANENTLY, Set.of());
    }

    public boolean canTransition(ExecutionStatus from, ExecutionStatus to) {
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    /** Validates a transition, throwing {@link InvalidStateTransitionException} if illegal. */
    public void assertCanTransition(ExecutionStatus from, ExecutionStatus to) {
        if (!canTransition(from, to)) {
            throw new InvalidStateTransitionException(
                    "Illegal execution transition: " + from + " -> " + to);
        }
    }
}
