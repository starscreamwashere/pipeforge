package com.pipeforge.pipeline.controller;

import com.pipeforge.TestcontainersConfiguration;
import com.pipeforge.auth.dto.AuthResponse;
import com.pipeforge.auth.dto.LoginRequest;
import com.pipeforge.auth.dto.SignupRequest;
import com.pipeforge.auth.entity.Role;
import com.pipeforge.auth.entity.User;
import com.pipeforge.auth.repository.UserRepository;
import com.pipeforge.pipeline.dto.CreatePipelineRequest;
import com.pipeforge.pipeline.dto.PipelineResponse;
import com.pipeforge.pipeline.dto.UpdatePipelineRequest;
import com.pipeforge.pipeline.entity.PipelineStatus;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PipelineControllerIT {

    @Autowired TestRestTemplate rest;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private String engineerToken() {
        String email = "eng-" + System.nanoTime() + "@pipeforge.dev";
        return rest.postForEntity("/api/v1/auth/signup",
                new SignupRequest("Eng", email, "password1"), AuthResponse.class).getBody().accessToken();
    }

    private String viewerToken() {
        String email = "viewer-" + System.nanoTime() + "@pipeforge.dev";
        User viewer = new User();
        viewer.setName("Vee");
        viewer.setEmail(email);
        viewer.setPasswordHash(passwordEncoder.encode("password1"));
        viewer.setRole(Role.VIEWER);
        userRepository.save(viewer);

        return rest.postForEntity("/api/v1/auth/login",
                new LoginRequest(email, "password1"), AuthResponse.class).getBody().accessToken();
    }

    private HttpHeaders auth(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    @Test
    void engineerCanRunFullCrud() {
        String token = engineerToken();

        // create
        ResponseEntity<PipelineResponse> created = rest.exchange(
                "/api/v1/pipelines", HttpMethod.POST,
                new HttpEntity<>(new CreatePipelineRequest("etl", "nightly", "0 0 2 * * *", null), auth(token)),
                PipelineResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).isNotNull();
        assertThat(created.getBody().status()).isEqualTo(PipelineStatus.DRAFT);
        var id = created.getBody().id();

        // list
        ResponseEntity<String> list = rest.exchange(
                "/api/v1/pipelines?page=0&size=10", HttpMethod.GET, new HttpEntity<>(auth(token)), String.class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(list.getBody()).contains("\"totalElements\"");

        // get
        ResponseEntity<PipelineResponse> got = rest.exchange(
                "/api/v1/pipelines/" + id, HttpMethod.GET, new HttpEntity<>(auth(token)), PipelineResponse.class);
        assertThat(got.getStatusCode()).isEqualTo(HttpStatus.OK);

        // update
        ResponseEntity<PipelineResponse> updated = rest.exchange(
                "/api/v1/pipelines/" + id, HttpMethod.PUT,
                new HttpEntity<>(new UpdatePipelineRequest("etl-renamed", null, null, null, PipelineStatus.ACTIVE),
                        auth(token)),
                PipelineResponse.class);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().name()).isEqualTo("etl-renamed");
        assertThat(updated.getBody().status()).isEqualTo(PipelineStatus.ACTIVE);

        // delete -> 204
        ResponseEntity<Void> deleted = rest.exchange(
                "/api/v1/pipelines/" + id, HttpMethod.DELETE, new HttpEntity<>(auth(token)), Void.class);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // get after delete -> 404
        ResponseEntity<String> afterDelete = rest.exchange(
                "/api/v1/pipelines/" + id, HttpMethod.GET, new HttpEntity<>(auth(token)), String.class);
        assertThat(afterDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createRequiresAuthentication() {
        ResponseEntity<String> response = rest.postForEntity(
                "/api/v1/pipelines", new CreatePipelineRequest("etl", null, null, null), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void viewerIsForbiddenFromCreating() {
        ResponseEntity<String> response = rest.exchange(
                "/api/v1/pipelines", HttpMethod.POST,
                new HttpEntity<>(new CreatePipelineRequest("etl", null, null, null), auth(viewerToken())),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
