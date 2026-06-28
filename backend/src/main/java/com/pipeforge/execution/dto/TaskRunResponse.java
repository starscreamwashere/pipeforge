package com.pipeforge.execution.dto;

import com.pipeforge.execution.entity.ExecutionStatus;

import java.time.Instant;
import java.util.UUID;

/** Public projection of a task run (App Flow §5.7 execution detail). */
public record TaskRunResponse(
        UUID id,
        UUID pipelineRunId,
        UUID taskId,
        String taskName,
        ExecutionStatus status,
        int attempt,
        String workerId,
        Instant startedAt,
        Instant completedAt,
        String errorMessage) {
}
