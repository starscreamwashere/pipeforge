package com.pipeforge.pipeline.controller;

import com.pipeforge.auth.security.AuthenticatedUser;
import com.pipeforge.common.dto.PageResponse;
import com.pipeforge.pipeline.dto.CreatePipelineRequest;
import com.pipeforge.pipeline.dto.PipelineResponse;
import com.pipeforge.pipeline.dto.UpdatePipelineRequest;
import com.pipeforge.pipeline.service.PipelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Pipeline CRUD API (Technical Requirements §11 — {@code /api/v1/pipelines}).
 *
 * <p>Reads are open to any authenticated user (incl. VIEWER); writes require
 * ENGINEER or ADMIN (TRD §3 authorization model).
 */
@RestController
@RequestMapping("/api/v1/pipelines")
@Tag(name = "Pipelines")
@SecurityRequirement(name = "bearerAuth")
public class PipelineController {

    private final PipelineService pipelineService;

    public PipelineController(PipelineService pipelineService) {
        this.pipelineService = pipelineService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ENGINEER', 'ADMIN')")
    @Operation(summary = "Create a pipeline")
    public PipelineResponse create(@Valid @RequestBody CreatePipelineRequest request,
                                   @AuthenticationPrincipal AuthenticatedUser actor) {
        return pipelineService.create(request, actor.id());
    }

    @GetMapping
    @Operation(summary = "List pipelines (paginated)")
    public PageResponse<PipelineResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return pipelineService.list(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a pipeline by id")
    public PipelineResponse get(@PathVariable UUID id) {
        return pipelineService.get(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ENGINEER', 'ADMIN')")
    @Operation(summary = "Update a pipeline")
    public PipelineResponse update(@PathVariable UUID id,
                                   @Valid @RequestBody UpdatePipelineRequest request) {
        return pipelineService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ENGINEER', 'ADMIN')")
    @Operation(summary = "Soft-delete a pipeline")
    public void delete(@PathVariable UUID id) {
        pipelineService.delete(id);
    }
}
