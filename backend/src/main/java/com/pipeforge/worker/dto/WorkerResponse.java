package com.pipeforge.worker.dto;

import com.pipeforge.worker.entity.WorkerStatus;

import java.time.Instant;
import java.util.UUID;

/** Public projection of a worker node (App Flow §5.9). */
public record WorkerResponse(
        UUID id,
        String name,
        WorkerStatus status,
        long jobsProcessed,
        UUID currentTaskRunId,
        Instant lastHeartbeatAt,
        Instant createdAt) {
}
