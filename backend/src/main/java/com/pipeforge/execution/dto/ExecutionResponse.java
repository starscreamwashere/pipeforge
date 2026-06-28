package com.pipeforge.execution.dto;

import com.pipeforge.execution.entity.ExecutionStatus;
import com.pipeforge.execution.entity.TriggerType;

import java.time.Instant;
import java.util.UUID;

/** Public projection of a pipeline run (App Flow §5.6 execution table). */
public record ExecutionResponse(
        UUID id,
        UUID pipelineId,
        ExecutionStatus status,
        TriggerType triggerType,
        Instant startedAt,
        Instant completedAt,
        String errorMessage,
        Instant createdAt) {
}
