package com.pipeforge.pipeline.dto;

import java.util.List;
import java.util.UUID;

/**
 * The full DAG for a pipeline: nodes, edges, and a valid execution order
 * (topological sort). Backs the DAG visualization (App Flow §5.4, UI/UX §10).
 */
public record DagResponse(
        UUID pipelineId,
        List<TaskResponse> tasks,
        List<DependencyResponse> dependencies,
        List<UUID> topologicalOrder) {
}
