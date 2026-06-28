package com.pipeforge.auth.security;

import com.pipeforge.auth.entity.Role;

import java.util.UUID;

/**
 * Authenticated principal placed in the {@code SecurityContext} by
 * {@link JwtAuthenticationFilter}. Derived entirely from the verified JWT
 * (no per-request DB lookup — the API is stateless).
 */
public record AuthenticatedUser(UUID id, String email, Role role) {
}
