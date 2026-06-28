package com.pipeforge.pipeline.dto;

import java.util.UUID;

/** Public projection of a DAG edge. */
public record DependencyResponse(
        UUID id,
        UUID pipelineId,
        UUID parentTaskId,
        UUID childTaskId) {
}
