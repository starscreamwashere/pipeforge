package com.pipeforge.worker.runtime;

import com.pipeforge.TestcontainersConfiguration;
import com.pipeforge.auth.dto.AuthResponse;
import com.pipeforge.auth.dto.SignupRequest;
import com.pipeforge.execution.dto.ExecutionDetailResponse;
import com.pipeforge.execution.dto.ExecutionResponse;
import com.pipeforge.execution.entity.ExecutionStatus;
import com.pipeforge.pipeline.dto.CreateDependencyRequest;
import com.pipeforge.pipeline.dto.CreatePipelineRequest;
import com.pipeforge.pipeline.dto.CreateTaskRequest;
import com.pipeforge.pipeline.dto.PipelineResponse;
import com.pipeforge.pipeline.dto.TaskResponse;
import com.pipeforge.pipeline.entity.RetryPolicy;
import com.pipeforge.pipeline.entity.TaskType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end worker runtime with the embedded consumer enabled
 * (Phase 7.3 "worker consumes jobs", 7.6 "jobs complete correctly"):
 * a triggered DAG is consumed from Redis and runs to completion.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "pipeforge.worker.consumer.enabled=true")
class WorkerRuntimeIT {

    @Autowired TestRestTemplate rest;

    private HttpHeaders auth;

    private void engineer() {
        String email = "wk-" + System.nanoTime() + "@pipeforge.dev";
        String token = rest.postForEntity("/api/v1/auth/signup",
                new SignupRequest("Eng", email, "password1"), AuthResponse.class).getBody().accessToken();
        auth = new HttpHeaders();
        auth.setBearerAuth(token);
    }

    private UUID createPipeline() {
        return createPipeline(null);
    }

    private UUID createPipeline(RetryPolicy retryPolicy) {
        return rest.exchange("/api/v1/pipelines", HttpMethod.POST,
                new HttpEntity<>(new CreatePipelineRequest("wk-pipe", null, null, retryPolicy), auth),
                PipelineResponse.class).getBody().id();
    }

    private UUID addTask(UUID pipelineId, String name, Map<String, Object> config) {
        return rest.exchange("/api/v1/pipelines/" + pipelineId + "/tasks", HttpMethod.POST,
                new HttpEntity<>(new CreateTaskRequest(name, TaskType.EXTRACT, config, null, null), auth),
                TaskResponse.class).getBody().id();
    }

    private ExecutionStatus pollRunStatus(UUID runId, Duration timeout) throws InterruptedException {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            ExecutionDetailResponse detail = rest.exchange("/api/v1/executions/" + runId,
                    HttpMethod.GET, new HttpEntity<>(auth), ExecutionDetailResponse.class).getBody();
            if (detail.execution().status().isTerminal()) {
                return detail.execution().status();
            }
            Thread.sleep(250);
        }
        return null;
    }

    @Test
    void consumerRunsAGraphToCompletion() throws InterruptedException {
        engineer();
        UUID pipelineId = createPipeline();
        UUID a = addTask(pipelineId, "extract", null);
        UUID b = addTask(pipelineId, "load", null);
        rest.exchange("/api/v1/pipelines/" + pipelineId + "/dependencies", HttpMethod.POST,
                new HttpEntity<>(new CreateDependencyRequest(a, b), auth), String.class);

        UUID runId = rest.exchange("/api/v1/pipelines/" + pipelineId + "/trigger", HttpMethod.POST,
                new HttpEntity<>(auth), ExecutionResponse.class).getBody().id();

        ExecutionStatus status = pollRunStatus(runId, Duration.ofSeconds(45));
        assertThat(status).isEqualTo(ExecutionStatus.SUCCESS);

        ExecutionDetailResponse detail = rest.exchange("/api/v1/executions/" + runId,
                HttpMethod.GET, new HttpEntity<>(auth), ExecutionDetailResponse.class).getBody();
        assertThat(detail.taskRuns()).allMatch(tr -> tr.status() == ExecutionStatus.SUCCESS);
    }

    @Test
    void consumerDeadLettersAFailingTask() throws InterruptedException {
        engineer();
        UUID pipelineId = createPipeline(new RetryPolicy(0, 1)); // no retries -> immediate dead-letter
        addTask(pipelineId, "boom", Map.of("fail", true));

        UUID runId = rest.exchange("/api/v1/pipelines/" + pipelineId + "/trigger", HttpMethod.POST,
                new HttpEntity<>(auth), ExecutionResponse.class).getBody().id();

        ExecutionStatus status = pollRunStatus(runId, Duration.ofSeconds(45));
        assertThat(status).isEqualTo(ExecutionStatus.FAILED_PERMANENTLY);
    }
}
