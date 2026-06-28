package com.pipeforge.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * Base for entities that support soft deletion via an {@code is_deleted} flag
 * (Backend Schema §3 — optional for pipelines and tasks).
 */
@Getter
@Setter
@MappedSuperclass
public abstract class SoftDeletableEntity extends AuditEntity {

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;
}
