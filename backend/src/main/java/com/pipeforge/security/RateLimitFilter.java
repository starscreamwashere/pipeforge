package com.pipeforge.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pipeforge.config.RateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Per-IP rate limiting for the sensitive endpoints (Technical Requirements §3):
 * authentication and execution triggers. Returns {@code 429} when exceeded.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter rateLimiter;
    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RateLimiter rateLimiter, RateLimitProperties properties, ObjectMapper objectMapper) {
        this.rateLimiter = rateLimiter;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String scope = scopeFor(request);
        if (properties.isEnabled() && scope != null) {
            int limit = "auth".equals(scope) ? properties.getAuthLimit() : properties.getTriggerLimit();
            String key = "ratelimit:%s:%s".formatted(scope, request.getRemoteAddr());
            if (!rateLimiter.allow(key, limit, Duration.ofSeconds(properties.getWindowSeconds()))) {
                writeTooManyRequests(request, response);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private String scopeFor(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.startsWith("/api/v1/auth/")) {
            return "auth";
        }
        if (path.startsWith("/api/v1/pipelines/") && path.endsWith("/trigger")) {
            return "trigger";
        }
        return null;
    }

    private void writeTooManyRequests(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of(
                "timestamp", Instant.now().toString(),
                "status", 429,
                "error", "RATE_LIMIT_EXCEEDED",
                "message", "Too many requests — please slow down",
                "path", request.getRequestURI()));
    }
}
