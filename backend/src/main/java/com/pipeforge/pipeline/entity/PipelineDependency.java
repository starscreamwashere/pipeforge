package com.pipeforge.pipeline.entity;

import com.pipeforge.common.entity.AuditEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * A DAG edge — {@code parentTask -> childTask}, meaning the child depends on
 * the parent (Backend Schema §5 — {@code pipeline_dependencies}).
 */
@Getter
@Setter
@Entity
@Table(name = "pipeline_dependencies")
public class PipelineDependency extends AuditEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pipeline_id", nullable = false)
    private Pipeline pipeline;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parent_task_id", nullable = false)
    private PipelineTask parentTask;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_task_id", nullable = false)
    private PipelineTask childTask;
}
