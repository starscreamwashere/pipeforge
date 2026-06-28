package com.pipeforge.auth.security;

import com.pipeforge.auth.entity.User;
import com.pipeforge.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Issues and verifies HS256 access tokens (Technical Requirements §3).
 *
 * <p>Refresh tokens are opaque random strings persisted in the database
 * (see the auth service) so they can be revoked; only the short-lived access
 * token is a JWT.
 */
@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(properties.getAccessTokenExpiry());
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                // Pin HS256 per Technical Requirements §3 (jjwt would otherwise
                // auto-select a stronger HMAC based on key length).
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /** Access-token lifetime in seconds (for the {@code expiresIn} response field). */
    public long getAccessTokenExpirySeconds() {
        return properties.getAccessTokenExpiry() / 1000;
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parse(token).getSubject());
    }
}
