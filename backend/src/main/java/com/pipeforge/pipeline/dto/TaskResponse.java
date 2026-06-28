package com.pipeforge.pipeline.dto;

import com.pipeforge.pipeline.entity.TaskType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Public projection of a pipeline task (DAG node). */
public record TaskResponse(
        UUID id,
        UUID pipelineId,
        String taskName,
        TaskType taskType,
        Map<String, Object> configPayload,
        Integer timeoutSeconds,
        Integer executionOrderHint,
        Instant createdAt,
        Instant updatedAt) {
}
