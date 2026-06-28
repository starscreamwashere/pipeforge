package com.pipeforge.pipeline.dto;

import com.pipeforge.pipeline.entity.PipelineStatus;
import com.pipeforge.pipeline.entity.RetryPolicy;
import jakarta.validation.constraints.Size;

/**
 * Payload for updating a pipeline. Null fields are left unchanged
 * (partial update).
 */
public record UpdatePipelineRequest(

        @Size(max = 150)
        String name,

        String description,

        @Size(max = 100)
        String scheduleCron,

        RetryPolicy retryPolicy,

        PipelineStatus status) {
}
