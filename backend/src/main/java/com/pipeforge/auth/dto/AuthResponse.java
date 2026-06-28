package com.pipeforge.auth.dto;

/**
 * Authentication result returned by signup / login / refresh
 * (Technical Requirements §3 — access + refresh tokens).
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserResponse user) {

    public static AuthResponse bearer(String accessToken, String refreshToken,
                                      long expiresIn, UserResponse user) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, user);
    }
}
