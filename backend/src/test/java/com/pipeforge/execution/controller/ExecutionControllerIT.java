package com.pipeforge.execution.controller;

import com.pipeforge.TestcontainersConfiguration;
import com.pipeforge.auth.dto.AuthResponse;
import com.pipeforge.auth.dto.SignupRequest;
import com.pipeforge.execution.dto.ExecutionDetailResponse;
import com.pipeforge.execution.dto.ExecutionResponse;
import com.pipeforge.execution.dto.TaskRunResponse;
import com.pipeforge.execution.entity.ExecutionStatus;
import com.pipeforge.execution.queue.QueueKeys;
import com.pipeforge.pipeline.dto.CreateDependencyRequest;
import com.pipeforge.pipeline.dto.CreatePipelineRequest;
import com.pipeforge.pipeline.dto.CreateTaskRequest;
import com.pipeforge.pipeline.dto.PipelineResponse;
import com.pipeforge.pipeline.dto.TaskResponse;
import com.pipeforge.pipeline.entity.TaskType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ExecutionControllerIT {

    @Autowired TestRestTemplate rest;
    @Autowired StringRedisTemplate redis;

    private HttpHeaders auth;

    private void engineer() {
        String email = "exec-" + System.nanoTime() + "@pipeforge.dev";
        String token = rest.postForEntity("/api/v1/auth/signup",
                new SignupRequest("Eng", email, "password1"), AuthResponse.class).getBody().accessToken();
        auth = new HttpHeaders();
        auth.setBearerAuth(token);
    }

    private UUID createPipeline() {
        return rest.exchange("/api/v1/pipelines", HttpMethod.POST,
                new HttpEntity<>(new CreatePipelineRequest("exec-pipe", null, null, null), auth),
                PipelineResponse.class).getBody().id();
    }

    private UUID addTask(UUID pipelineId, String name, TaskType type) {
        return rest.exchange("/api/v1/pipelines/" + pipelineId + "/tasks", HttpMethod.POST,
                new HttpEntity<>(new CreateTaskRequest(name, type, null, null, null), auth),
                TaskResponse.class).getBody().id();
    }

    @Test
    void triggerCreatesRunQueuesRootsAndPublishesToRedis() {
        engineer();
        UUID pipelineId = createPipeline();
        UUID a = addTask(pipelineId, "extract", TaskType.EXTRACT);
        UUID b = addTask(pipelineId, "load", TaskType.LOAD);
        // A -> B : A is the root, B depends on A
        rest.exchange("/api/v1/pipelines/" + pipelineId + "/dependencies", HttpMethod.POST,
                new HttpEntity<>(new CreateDependencyRequest(a, b), auth), String.class);

        // trigger -> 202, run QUEUED
        ResponseEntity<ExecutionResponse> triggered = rest.exchange(
                "/api/v1/pipelines/" + pipelineId + "/trigger", HttpMethod.POST,
                new HttpEntity<>(auth), ExecutionResponse.class);
        assertThat(triggered.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(triggered.getBody().status()).isEqualTo(ExecutionStatus.QUEUED);
        UUID runId = triggered.getBody().id();

        // detail: 2 task runs; root A QUEUED, child B PENDING
        ExecutionDetailResponse detail = rest.exchange("/api/v1/executions/" + runId, HttpMethod.GET,
                new HttpEntity<>(auth), ExecutionDetailResponse.class).getBody();
        assertThat(detail.taskRuns()).hasSize(2);
        TaskRunResponse rootRun = detail.taskRuns().stream()
                .filter(tr -> tr.taskId().equals(a)).findFirst().orElseThrow();
        TaskRunResponse childRun = detail.taskRuns().stream()
                .filter(tr -> tr.taskId().equals(b)).findFirst().orElseThrow();
        assertThat(rootRun.status()).isEqualTo(ExecutionStatus.QUEUED);
        assertThat(childRun.status()).isEqualTo(ExecutionStatus.PENDING);

        // the root task-run id was published to the Redis ready queue
        List<String> ready = redis.opsForList().range(QueueKeys.READY_QUEUE, 0, -1);
        assertThat(ready).contains(rootRun.id().toString());
    }

    @Test
    void triggerEmptyPipelineIsRejected() {
        engineer();
        UUID pipelineId = createPipeline();

        ResponseEntity<String> response = rest.exchange(
                "/api/v1/pipelines/" + pipelineId + "/trigger", HttpMethod.POST,
                new HttpEntity<>(auth), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void cancelTransitionsRunToCancelled() {
        engineer();
        UUID pipelineId = createPipeline();
        addTask(pipelineId, "solo", TaskType.CUSTOM);
        UUID runId = rest.exchange("/api/v1/pipelines/" + pipelineId + "/trigger", HttpMethod.POST,
                new HttpEntity<>(auth), ExecutionResponse.class).getBody().id();

        ResponseEntity<ExecutionResponse> cancelled = rest.exchange(
                "/api/v1/executions/" + runId + "/cancel", HttpMethod.POST,
                new HttpEntity<>(auth), ExecutionResponse.class);
        assertThat(cancelled.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(cancelled.getBody().status()).isEqualTo(ExecutionStatus.CANCELLED);
    }
}
