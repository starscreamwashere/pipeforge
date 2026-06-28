package com.pipeforge.pipeline.controller;

import com.pipeforge.TestcontainersConfiguration;
import com.pipeforge.auth.dto.AuthResponse;
import com.pipeforge.auth.dto.SignupRequest;
import com.pipeforge.pipeline.dto.CreateDependencyRequest;
import com.pipeforge.pipeline.dto.CreatePipelineRequest;
import com.pipeforge.pipeline.dto.CreateTaskRequest;
import com.pipeforge.pipeline.dto.DagResponse;
import com.pipeforge.pipeline.dto.PipelineResponse;
import com.pipeforge.pipeline.dto.TaskResponse;
import com.pipeforge.pipeline.entity.TaskType;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DagControllerIT {

    @Autowired TestRestTemplate rest;

    private HttpHeaders auth;
    private UUID pipelineId;

    private void setUpEngineerPipeline() {
        String email = "dag-" + System.nanoTime() + "@pipeforge.dev";
        String token = rest.postForEntity("/api/v1/auth/signup",
                new SignupRequest("Eng", email, "password1"), AuthResponse.class).getBody().accessToken();
        auth = new HttpHeaders();
        auth.setBearerAuth(token);
        pipelineId = rest.exchange("/api/v1/pipelines", HttpMethod.POST,
                new HttpEntity<>(new CreatePipelineRequest("dag-pipe", null, null, null), auth),
                PipelineResponse.class).getBody().id();
    }

    private UUID addTask(String name) {
        return rest.exchange("/api/v1/pipelines/" + pipelineId + "/tasks", HttpMethod.POST,
                new HttpEntity<>(new CreateTaskRequest(name, TaskType.EXTRACT, null, null, null), auth),
                TaskResponse.class).getBody().id();
    }

    private ResponseEntity<String> addDependency(UUID parent, UUID child) {
        return rest.exchange("/api/v1/pipelines/" + pipelineId + "/dependencies", HttpMethod.POST,
                new HttpEntity<>(new CreateDependencyRequest(parent, child), auth), String.class);
    }

    @Test
    void buildsDagEnforcesAcyclicityAndReturnsTopologicalOrder() {
        setUpEngineerPipeline();
        UUID a = addTask("A");
        UUID b = addTask("B");
        UUID c = addTask("C");

        assertThat(addDependency(a, b).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(addDependency(b, c).getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // DAG with topological order A before B before C
        DagResponse dag = rest.exchange("/api/v1/pipelines/" + pipelineId + "/dag", HttpMethod.GET,
                new HttpEntity<>(auth), DagResponse.class).getBody();
        assertThat(dag.tasks()).hasSize(3);
        assertThat(dag.dependencies()).hasSize(2);
        assertThat(dag.topologicalOrder().indexOf(a)).isLessThan(dag.topologicalOrder().indexOf(b));
        assertThat(dag.topologicalOrder().indexOf(b)).isLessThan(dag.topologicalOrder().indexOf(c));

        // Cycle-creating edge C -> A is rejected
        ResponseEntity<String> cycle = addDependency(c, a);
        assertThat(cycle.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(cycle.getBody()).contains("PIPELINE_CYCLE");

        // Self-dependency is rejected
        assertThat(addDependency(a, a).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // validate -> 200
        ResponseEntity<Void> validate = rest.exchange(
                "/api/v1/pipelines/" + pipelineId + "/dag/validate", HttpMethod.POST,
                new HttpEntity<>(auth), Void.class);
        assertThat(validate.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
