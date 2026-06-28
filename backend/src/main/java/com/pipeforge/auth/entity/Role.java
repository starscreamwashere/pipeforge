package com.pipeforge.auth.entity;

/**
 * Authorization roles (PRD §5.1, Backend Schema §4).
 *
 * <ul>
 *   <li>{@code ADMIN} — full system access, worker + failure-recovery controls</li>
 *   <li>{@code ENGINEER} — pipeline CRUD, execution management, monitoring</li>
 *   <li>{@code VIEWER} — read-only dashboards, logs, metrics</li>
 * </ul>
 */
public enum Role {
    ADMIN,
    ENGINEER,
    VIEWER
}
