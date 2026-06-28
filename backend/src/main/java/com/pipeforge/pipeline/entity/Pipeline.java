package com.pipeforge.pipeline.entity;

import com.pipeforge.auth.entity.User;
import com.pipeforge.common.entity.SoftDeletableEntity;
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
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

/**
 * A pipeline definition (Backend Schema §5 — {@code pipelines}).
 *
 * <p>Soft-deletable: {@link SQLRestriction} hides rows with {@code is_deleted = true}
 * from all queries, so deletion is a flag flip rather than a row removal.
 */
@Getter
@Setter
@Entity
@Table(name = "pipelines")
@SQLRestriction("is_deleted = false")
public class Pipeline extends SoftDeletableEntity {

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "schedule_cron", length = 100)
    private String scheduleCron;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "retry_policy")
    private RetryPolicy retryPolicy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PipelineStatus status = PipelineStatus.DRAFT;

    @Column(name = "version", nullable = false)
    private Integer version = 1;
}
