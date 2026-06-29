package com.pipeforge.worker.controller;

import com.pipeforge.worker.dto.WorkerResponse;
import com.pipeforge.worker.service.WorkerQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Worker monitoring API (Technical Requirements §11). Read-only, any authenticated user. */
@RestController
@RequestMapping("/api/v1/workers")
@Tag(name = "Workers")
@SecurityRequirement(name = "bearerAuth")
public class WorkerController {

    private final WorkerQueryService workerQueryService;

    public WorkerController(WorkerQueryService workerQueryService) {
        this.workerQueryService = workerQueryService;
    }

    @GetMapping
    @Operation(summary = "List worker nodes")
    public List<WorkerResponse> list() {
        return workerQueryService.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a worker by id")
    public WorkerResponse get(@PathVariable UUID id) {
        return workerQueryService.get(id);
    }
}
