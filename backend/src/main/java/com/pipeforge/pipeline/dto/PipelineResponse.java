package com.pipeforge.pipeline.dto;

import com.pipeforge.pipeline.entity.PipelineStatus;
import com.pipeforge.pipeline.entity.RetryPolicy;

import java.time.Instant;
import java.util.UUID;

/** Public projection of a pipeline. */
public record PipelineResponse(
        UUID id,
        String name,
        String description,
        String scheduleCron,
        RetryPolicy retryPolicy,
        UUID ownerId,
        String ownerName,
        PipelineStatus status,
        Integer version,
        Instant createdAt,
        Instant updatedAt) {
}
