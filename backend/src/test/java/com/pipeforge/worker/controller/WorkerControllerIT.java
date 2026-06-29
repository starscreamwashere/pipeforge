package com.pipeforge.worker.controller;

import com.pipeforge.TestcontainersConfiguration;
import com.pipeforge.auth.dto.AuthResponse;
import com.pipeforge.auth.dto.SignupRequest;
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

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WorkerControllerIT {

    @Autowired TestRestTemplate rest;

    @Test
    void listsWorkersForAuthenticatedUser() {
        String token = rest.postForEntity("/api/v1/auth/signup",
                new SignupRequest("Eng", "wkc-" + System.nanoTime() + "@pipeforge.dev", "password1"),
                AuthResponse.class).getBody().accessToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<String> response = rest.exchange(
                "/api/v1/workers", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).startsWith("[");
    }

    @Test
    void requiresAuthentication() {
        ResponseEntity<String> response = rest.getForEntity("/api/v1/workers", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
