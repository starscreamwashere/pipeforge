package com.pipeforge.pipeline.controller;

import com.pipeforge.pipeline.dto.CreateDependencyRequest;
import com.pipeforge.pipeline.dto.CreateTaskRequest;
import com.pipeforge.pipeline.dto.DagResponse;
import com.pipeforge.pipeline.dto.DependencyResponse;
import com.pipeforge.pipeline.dto.TaskResponse;
import com.pipeforge.pipeline.service.DagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * DAG construction API for a pipeline (Technical Requirements §11).
 * Writes require ENGINEER/ADMIN; reads are open to any authenticated user.
 */
@RestController
@RequestMapping("/api/v1/pipelines/{pipelineId}")
@Tag(name = "DAG")
@SecurityRequirement(name = "bearerAuth")
public class DagController {

    private final DagService dagService;

    public DagController(DagService dagService) {
        this.dagService = dagService;
    }

    @PostMapping("/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ENGINEER', 'ADMIN')")
    @Operation(summary = "Add a task (DAG node) to a pipeline")
    public TaskResponse addTask(@PathVariable UUID pipelineId, @Valid @RequestBody CreateTaskRequest request) {
        return dagService.addTask(pipelineId, request);
    }

    @GetMapping("/tasks")
    @Operation(summary = "List a pipeline's tasks")
    public List<TaskResponse> listTasks(@PathVariable UUID pipelineId) {
        return dagService.listTasks(pipelineId);
    }

    @DeleteMapping("/tasks/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ENGINEER', 'ADMIN')")
    @Operation(summary = "Delete a task")
    public void deleteTask(@PathVariable UUID pipelineId, @PathVariable UUID taskId) {
        dagService.deleteTask(pipelineId, taskId);
    }

    @PostMapping("/dependencies")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ENGINEER', 'ADMIN')")
    @Operation(summary = "Add a dependency edge (rejected if it creates a cycle)")
    public DependencyResponse addDependency(@PathVariable UUID pipelineId,
                                            @Valid @RequestBody CreateDependencyRequest request) {
        return dagService.addDependency(pipelineId, request);
    }

    @DeleteMapping("/dependencies/{dependencyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ENGINEER', 'ADMIN')")
    @Operation(summary = "Delete a dependency edge")
    public void deleteDependency(@PathVariable UUID pipelineId, @PathVariable UUID dependencyId) {
        dagService.deleteDependency(pipelineId, dependencyId);
    }

    @GetMapping("/dag")
    @Operation(summary = "Get the full DAG with a topological execution order")
    public DagResponse getDag(@PathVariable UUID pipelineId) {
        return dagService.getDag(pipelineId);
    }

    @PostMapping("/dag/validate")
    @PreAuthorize("hasAnyRole('ENGINEER', 'ADMIN')")
    @Operation(summary = "Validate the DAG (400 if a cycle exists)")
    public void validate(@PathVariable UUID pipelineId) {
        dagService.validate(pipelineId);
    }
}
