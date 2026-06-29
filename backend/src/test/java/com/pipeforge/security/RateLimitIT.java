package com.pipeforge.security;

import com.pipeforge.TestcontainersConfiguration;
import com.pipeforge.auth.dto.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies Redis-backed rate limiting on auth endpoints (Phase 10.4). The
 * auth limit is lowered to 3 for this context.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "pipeforge.security.rate-limit.auth-limit=3")
class RateLimitIT {

    @Autowired
    TestRestTemplate rest;

    @Test
    void blocksRequestsBeyondTheAuthLimit() {
        LoginRequest body = new LoginRequest("nobody@pipeforge.dev", "whatever1");

        HttpStatus first = post(body);
        HttpStatus second = post(body);
        HttpStatus third = post(body);
        HttpStatus fourth = post(body);

        // First three are processed (401 — no such user); the fourth is rate-limited.
        assertThat(first).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(second).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(third).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(fourth).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    private HttpStatus post(LoginRequest body) {
        return HttpStatus.valueOf(
                rest.postForEntity("/api/v1/auth/login", body, String.class).getStatusCode().value());
    }
}
