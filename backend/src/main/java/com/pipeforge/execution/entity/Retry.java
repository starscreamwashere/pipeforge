package com.pipeforge.execution.entity;

import com.pipeforge.common.entity.AuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * A durable record of a scheduled retry for a task run (PRD §5.7).
 * The actual delayed re-enqueue lives in the Redis retry queue.
 */
@Getter
@Setter
@Entity
@Table(name = "retries")
public class Retry extends AuditEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_run_id", nullable = false)
    private TaskRun taskRun;

    @Column(name = "attempt", nullable = false)
    private int attempt;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "reason")
    private String reason;
}
