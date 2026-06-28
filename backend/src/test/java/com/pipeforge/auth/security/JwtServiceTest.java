package com.pipeforge.auth.security;

import com.pipeforge.auth.entity.Role;
import com.pipeforge.auth.entity.User;
import com.pipeforge.config.JwtProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JWT issuance + verification (Phase 2.5 — "token generation works").
 */
class JwtServiceTest {

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-0123456789-0123456789-abcdef");
        properties.setAccessTokenExpiry(60_000L);
        jwtService = new JwtService(properties);

        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("ada@pipeforge.dev");
        user.setRole(Role.ADMIN);
    }

    @Test
    void generatesAndParsesTokenWithClaims() {
        String token = jwtService.generateAccessToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.isValid(token)).isTrue();

        Claims claims = jwtService.parse(token);
        assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
        assertThat(claims.get("email", String.class)).isEqualTo("ada@pipeforge.dev");
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
        assertThat(jwtService.extractUserId(token)).isEqualTo(user.getId());
    }

    @Test
    void rejectsTamperedToken() {
        String token = jwtService.generateAccessToken(user);
        assertThat(jwtService.isValid(token + "tampered")).isFalse();
    }

    @Test
    void rejectsTokenSignedWithDifferentSecret() {
        JwtProperties other = new JwtProperties();
        other.setSecret("a-completely-different-secret-key-9876543210");
        String foreignToken = new JwtService(other).generateAccessToken(user);

        assertThat(jwtService.isValid(foreignToken)).isFalse();
    }
}
