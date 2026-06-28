package com.pipeforge.pipeline.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Payload for adding a DAG edge: {@code parentTask -> childTask}. */
public record CreateDependencyRequest(

        @NotNull
        UUID parentTaskId,

        @NotNull
        UUID childTaskId) {
}
