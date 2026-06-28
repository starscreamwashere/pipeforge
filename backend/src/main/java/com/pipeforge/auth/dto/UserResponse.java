package com.pipeforge.auth.dto;

import com.pipeforge.auth.entity.Role;

import java.util.UUID;

/** Public projection of a user (never exposes the password hash). */
public record UserResponse(
        UUID id,
        String name,
        String email,
        Role role,
        boolean active) {
}
