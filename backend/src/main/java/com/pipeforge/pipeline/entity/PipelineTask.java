package com.pipeforge.pipeline.entity;

import com.pipeforge.common.entity.AuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

/**
 * A DAG node — one unit of work within a pipeline
 * (Backend Schema §5 — {@code pipeline_tasks}).
 */
@Getter
@Setter
@Entity
@Table(name = "pipeline_tasks")
public class PipelineTask extends AuditEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pipeline_id", nullable = false)
    private Pipeline pipeline;

    @Column(name = "task_name", nullable = false, length = 150)
    private String taskName;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 20)
    private TaskType taskType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_payload")
    private Map<String, Object> configPayload;

    @Column(name = "timeout_seconds")
    private Integer timeoutSeconds;

    @Column(name = "execution_order_hint")
    private Integer executionOrderHint;
}
