package com.pipeforge.pipeline.dto;

import com.pipeforge.pipeline.entity.TaskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Map;

/** Payload for adding a task to a pipeline (App Flow §5.3 step 2). */
public record CreateTaskRequest(

        @NotBlank
        @Size(max = 150)
        String taskName,

        @NotNull
        TaskType taskType,

        Map<String, Object> configPayload,

        @Positive
        Integer timeoutSeconds,

        Integer executionOrderHint) {
}
