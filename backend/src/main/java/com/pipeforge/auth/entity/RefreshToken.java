package com.pipeforge.auth.entity;

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
 * A persisted JWT refresh token (Backend Schema §4 — {@code refresh_tokens}).
 *
 * <p>Unidirectional {@code @ManyToOne} to {@link User} (Backend Schema §10 —
 * "avoid bidirectional relationships unless necessary").
 */
@Getter
@Setter
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken extends AuditEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token", nullable = false)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;
}
