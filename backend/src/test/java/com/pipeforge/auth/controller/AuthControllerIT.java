package com.pipeforge.auth.controller;

import com.pipeforge.TestcontainersConfiguration;
import com.pipeforge.auth.dto.AuthResponse;
import com.pipeforge.auth.dto.LoginRequest;
import com.pipeforge.auth.dto.RefreshRequest;
import com.pipeforge.auth.dto.SignupRequest;
import com.pipeforge.auth.dto.UserResponse;
import com.pipeforge.auth.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end auth flow over HTTP (Phase 2.7 — "auth flow works"):
 * signup -> me -> login -> refresh -> unauthorized -> logout.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerIT {

    @Autowired
    TestRestTemplate rest;

    @Test
    void fullAuthenticationFlow() {
        String email = "flow-" + System.nanoTime() + "@pipeforge.dev";

        // signup -> 201 with tokens
        ResponseEntity<AuthResponse> signup = rest.postForEntity(
                "/api/v1/auth/signup", new SignupRequest("Flow User", email, "password1"), AuthResponse.class);
        assertThat(signup.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        AuthResponse tokens = signup.getBody();
        assertThat(tokens).isNotNull();
        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.refreshToken()).isNotBlank();
        assertThat(tokens.user().role()).isEqualTo(Role.ENGINEER);

        // me with bearer -> 200, correct email
        ResponseEntity<UserResponse> me = rest.exchange(
                "/api/v1/auth/me", HttpMethod.GET, bearer(tokens.accessToken()), UserResponse.class);
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody()).isNotNull();
        assertThat(me.getBody().email()).isEqualTo(email);

        // login -> 200
        ResponseEntity<AuthResponse> login = rest.postForEntity(
                "/api/v1/auth/login", new LoginRequest(email, "password1"), AuthResponse.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(login.getBody()).isNotNull();

        // refresh -> 200 with a new access token
        ResponseEntity<AuthResponse> refresh = rest.postForEntity(
                "/api/v1/auth/refresh", new RefreshRequest(login.getBody().refreshToken()), AuthResponse.class);
        assertThat(refresh.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refresh.getBody()).isNotNull();
        assertThat(refresh.getBody().accessToken()).isNotBlank();

        // me without token -> 401
        ResponseEntity<String> unauthorized = rest.getForEntity("/api/v1/auth/me", String.class);
        assertThat(unauthorized.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // logout -> 204
        ResponseEntity<Void> logout = rest.exchange(
                "/api/v1/auth/logout", HttpMethod.POST,
                new HttpEntity<>(new RefreshRequest(refresh.getBody().refreshToken())), Void.class);
        assertThat(logout.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    private HttpEntity<Void> bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }
}
