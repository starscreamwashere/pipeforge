package com.pipeforge.worker.entity;

import com.pipeforge.common.entity.AuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * A worker node that consumes and executes jobs (Backend Schema §5 — {@code workers}).
 */
@Getter
@Setter
@Entity
@Table(name = "workers")
public class Worker extends AuditEntity {

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WorkerStatus status = WorkerStatus.ACTIVE;

    @Column(name = "jobs_processed", nullable = false)
    private long jobsProcessed = 0;

    @Column(name = "current_task_run_id")
    private UUID currentTaskRunId;

    @Column(name = "last_heartbeat_at")
    private Instant lastHeartbeatAt;
}
