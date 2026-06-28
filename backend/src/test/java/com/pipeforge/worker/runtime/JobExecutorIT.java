package com.pipeforge.worker.runtime;

import com.pipeforge.TestcontainersConfiguration;
import com.pipeforge.auth.entity.Role;
import com.pipeforge.auth.entity.User;
import com.pipeforge.auth.repository.UserRepository;
import com.pipeforge.execution.entity.ExecutionStatus;
import com.pipeforge.execution.entity.TaskRun;
import com.pipeforge.execution.entity.TriggerType;
import com.pipeforge.execution.queue.QueueKeys;
import com.pipeforge.execution.repository.PipelineRunRepository;
import com.pipeforge.execution.repository.TaskRunRepository;
import com.pipeforge.execution.service.ExecutionService;
import com.pipeforge.pipeline.entity.Pipeline;
import com.pipeforge.pipeline.entity.PipelineDependency;
import com.pipeforge.pipeline.entity.PipelineStatus;
import com.pipeforge.pipeline.entity.PipelineTask;
import com.pipeforge.pipeline.entity.RetryPolicy;
import com.pipeforge.pipeline.entity.TaskType;
import com.pipeforge.pipeline.repository.PipelineDependencyRepository;
import com.pipeforge.pipeline.repository.PipelineRepository;
import com.pipeforge.pipeline.repository.PipelineTaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Deterministic job-execution tests by invoking {@link JobExecutor#execute}
 * directly (Phase 7.6 "jobs complete correctly", 7.4 "duplicate execution
 * impossible"). Not transactional, so each execute commits before assertions.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class JobExecutorIT {

    @Autowired UserRepository userRepository;
    @Autowired PipelineRepository pipelineRepository;
    @Autowired PipelineTaskRepository taskRepository;
    @Autowired PipelineDependencyRepository dependencyRepository;
    @Autowired ExecutionService executionService;
    @Autowired TaskRunRepository taskRunRepository;
    @Autowired PipelineRunRepository pipelineRunRepository;
    @Autowired JobExecutor jobExecutor;
    @Autowired DistributedLock distributedLock;
    @Autowired StringRedisTemplate redis;

    private Pipeline newPipeline(RetryPolicy retryPolicy) {
        User owner = new User();
        owner.setName("Owner");
        owner.setEmail("owner-" + System.nanoTime() + "@pipeforge.dev");
        owner.setPasswordHash("hash");
        owner.setRole(Role.ENGINEER);
        userRepository.save(owner);

        Pipeline pipeline = new Pipeline();
        pipeline.setName("job-pipe");
        pipeline.setOwner(owner);
        pipeline.setStatus(PipelineStatus.DRAFT);
        pipeline.setRetryPolicy(retryPolicy);
        return pipelineRepository.save(pipeline);
    }

    private PipelineTask newTask(Pipeline pipeline, String name, Map<String, Object> config) {
        PipelineTask task = new PipelineTask();
        task.setPipeline(pipeline);
        task.setTaskName(name);
        task.setTaskType(TaskType.EXTRACT);
        task.setConfigPayload(config);
        return taskRepository.save(task);
    }

    private TaskRun taskRunFor(UUID runId, UUID taskId) {
        return taskRunRepository.findByPipelineRunId(runId).stream()
                .filter(tr -> tr.getTask().getId().equals(taskId)).findFirst().orElseThrow();
    }

    @Test
    void runsDagToCompletionInDependencyOrder() {
        Pipeline pipeline = newPipeline(RetryPolicy.defaults());
        PipelineTask a = newTask(pipeline, "extract", null);
        PipelineTask b = newTask(pipeline, "load", null);
        PipelineDependency edge = new PipelineDependency();
        edge.setPipeline(pipeline);
        edge.setParentTask(a);
        edge.setChildTask(b);
        dependencyRepository.save(edge);

        UUID runId = executionService.trigger(pipeline.getId(), TriggerType.MANUAL).id();

        // Only the root A is queued; B waits.
        assertThat(taskRunFor(runId, a.getId()).getStatus()).isEqualTo(ExecutionStatus.QUEUED);
        assertThat(taskRunFor(runId, b.getId()).getStatus()).isEqualTo(ExecutionStatus.PENDING);

        // Execute A -> SUCCESS, B becomes QUEUED (eligible).
        jobExecutor.execute(taskRunFor(runId, a.getId()).getId());
        assertThat(taskRunFor(runId, a.getId()).getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
        assertThat(taskRunFor(runId, b.getId()).getStatus()).isEqualTo(ExecutionStatus.QUEUED);

        // Execute B -> SUCCESS, whole run SUCCESS.
        jobExecutor.execute(taskRunFor(runId, b.getId()).getId());
        assertThat(taskRunFor(runId, b.getId()).getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
        assertThat(pipelineRunRepository.findById(runId).orElseThrow().getStatus())
                .isEqualTo(ExecutionStatus.SUCCESS);
    }

    @Test
    void lockPreventsDuplicateExecution() {
        Pipeline pipeline = newPipeline(RetryPolicy.defaults());
        PipelineTask a = newTask(pipeline, "solo", null);
        UUID runId = executionService.trigger(pipeline.getId(), TriggerType.MANUAL).id();
        UUID taskRunId = taskRunFor(runId, a.getId()).getId();

        // A different worker holds the job lock -> execute must no-op.
        String lockKey = distributedLock.jobLockKey(taskRunId);
        assertThat(distributedLock.acquire(lockKey, "other-worker", Duration.ofSeconds(30))).isTrue();

        jobExecutor.execute(taskRunId);
        assertThat(taskRunFor(runId, a.getId()).getStatus()).isEqualTo(ExecutionStatus.QUEUED); // untouched

        // Release and retry -> now it runs.
        distributedLock.release(lockKey, "other-worker");
        jobExecutor.execute(taskRunId);
        assertThat(taskRunFor(runId, a.getId()).getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    }

    @Test
    void deadLettersWhenRetriesExhausted() {
        Pipeline pipeline = newPipeline(new RetryPolicy(0, 5)); // no retries
        PipelineTask a = newTask(pipeline, "failing", Map.of("fail", true));
        UUID runId = executionService.trigger(pipeline.getId(), TriggerType.MANUAL).id();

        jobExecutor.execute(taskRunFor(runId, a.getId()).getId());

        assertThat(taskRunFor(runId, a.getId()).getStatus()).isEqualTo(ExecutionStatus.FAILED_PERMANENTLY);
        assertThat(pipelineRunRepository.findById(runId).orElseThrow().getStatus())
                .isEqualTo(ExecutionStatus.FAILED_PERMANENTLY);
    }

    @Test
    void schedulesRetryOnFailureWhenAttemptsRemain() {
        Pipeline pipeline = newPipeline(new RetryPolicy(2, 5));
        PipelineTask a = newTask(pipeline, "flaky", Map.of("fail", true));
        UUID runId = executionService.trigger(pipeline.getId(), TriggerType.MANUAL).id();
        UUID taskRunId = taskRunFor(runId, a.getId()).getId();

        jobExecutor.execute(taskRunId);

        TaskRun afterFailure = taskRunFor(runId, a.getId());
        assertThat(afterFailure.getStatus()).isEqualTo(ExecutionStatus.RETRYING);
        assertThat(afterFailure.getAttempt()).isEqualTo(1);
        // queued on the Redis retry sorted-set
        assertThat(redis.opsForZSet().score(QueueKeys.RETRY_QUEUE, taskRunId.toString())).isNotNull();
    }
}
