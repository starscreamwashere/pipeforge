package com.pipeforge.pipeline.dto;

import com.pipeforge.pipeline.entity.RetryPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Payload for creating a pipeline (App Flow §5.3 step 1, Backend Schema §5). */
public record CreatePipelineRequest(

        @NotBlank
        @Size(max = 150)
        String name,

        String description,

        @Size(max = 100)
        String scheduleCron,

        RetryPolicy retryPolicy) {
}
