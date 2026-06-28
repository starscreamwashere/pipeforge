package com.pipeforge.execution.controller;

import com.pipeforge.common.dto.PageResponse;
import com.pipeforge.execution.dto.ExecutionDetailResponse;
import com.pipeforge.execution.dto.ExecutionResponse;
import com.pipeforge.execution.dto.TriggerExecutionRequest;
import com.pipeforge.execution.entity.TriggerType;
import com.pipeforge.execution.service.ExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Execution APIs (Technical Requirements §11): trigger pipelines and monitor
 * runs. Triggering requires ENGINEER/ADMIN; monitoring is open to any
 * authenticated user.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Executions")
@SecurityRequirement(name = "bearerAuth")
public class ExecutionController {

    private final ExecutionService executionService;

    public ExecutionController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    @PostMapping("/pipelines/{pipelineId}/trigger")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyRole('ENGINEER', 'ADMIN')")
    @Operation(summary = "Trigger a pipeline execution")
    public ExecutionResponse trigger(@PathVariable UUID pipelineId,
                                     @RequestBody(required = false) TriggerExecutionRequest request) {
        TriggerType triggerType = request == null ? TriggerType.MANUAL : request.triggerType();
        return executionService.trigger(pipelineId, triggerType);
    }

    @GetMapping("/executions")
    @Operation(summary = "List executions (most recent first)")
    public PageResponse<ExecutionResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return executionService.list(pageable);
    }

    @GetMapping("/executions/{runId}")
    @Operation(summary = "Get an execution with its task runs")
    public ExecutionDetailResponse get(@PathVariable UUID runId) {
        return executionService.getDetail(runId);
    }

    @PostMapping("/executions/{runId}/cancel")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyRole('ENGINEER', 'ADMIN')")
    @Operation(summary = "Cancel a running or queued execution")
    public ExecutionResponse cancel(@PathVariable UUID runId) {
        return executionService.cancel(runId);
    }
}
