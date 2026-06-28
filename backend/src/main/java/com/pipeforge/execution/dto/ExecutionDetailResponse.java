package com.pipeforge.execution.dto;

import java.util.List;

/** Execution metadata plus its per-task runs (App Flow §5.7). */
public record ExecutionDetailResponse(
        ExecutionResponse execution,
        List<TaskRunResponse> taskRuns) {
}
