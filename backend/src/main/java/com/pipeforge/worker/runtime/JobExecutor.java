package com.pipeforge.worker.runtime;

import com.pipeforge.execution.entity.ExecutionStatus;
import com.pipeforge.execution.entity.PipelineRun;
import com.pipeforge.execution.entity.TaskRun;
import com.pipeforge.execution.queue.QueuePublisher;
import com.pipeforge.execution.repository.PipelineRunRepository;
import com.pipeforge.execution.repository.TaskRunRepository;
import com.pipeforge.execution.retry.RetryService;
import com.pipeforge.execution.state.ExecutionStateMachine;
import com.pipeforge.metrics.MetricsService;
import com.pipeforge.pipeline.entity.PipelineDependency;
import com.pipeforge.pipeline.repository.PipelineDependencyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Executes one task run end-to-end (Technical Requirements §8):
 * claim (distributed lock) → run → update DB → release lock. On success it
 * enqueues newly-eligible downstream tasks; on failure it delegates to
 * {@link RetryService}.
 */
@Service
public class JobExecutor {

    private static final Logger log = LoggerFactory.getLogger(JobExecutor.class);
    private static final Duration LOCK_TTL = Duration.ofSeconds(60);

    private final TaskRunRepository taskRunRepository;
    private final PipelineRunRepository pipelineRunRepository;
    private final PipelineDependencyRepository dependencyRepository;
    private final QueuePublisher queuePublisher;
    private final DistributedLock distributedLock;
    private final RetryService retryService;
    private final ExecutionStateMachine stateMachine;
    private final TaskRunner taskRunner;
    private final WorkerIdentity workerIdentity;
    private final MetricsService metricsService;

    public JobExecutor(TaskRunRepository taskRunRepository,
                       PipelineRunRepository pipelineRunRepository,
                       PipelineDependencyRepository dependencyRepository,
                       QueuePublisher queuePublisher,
                       DistributedLock distributedLock,
                       RetryService retryService,
                       ExecutionStateMachine stateMachine,
                       TaskRunner taskRunner,
                       WorkerIdentity workerIdentity,
                       MetricsService metricsService) {
        this.taskRunRepository = taskRunRepository;
        this.pipelineRunRepository = pipelineRunRepository;
        this.dependencyRepository = dependencyRepository;
        this.queuePublisher = queuePublisher;
        this.distributedLock = distributedLock;
        this.retryService = retryService;
        this.stateMachine = stateMachine;
        this.taskRunner = taskRunner;
        this.workerIdentity = workerIdentity;
        this.metricsService = metricsService;
    }

    @Transactional
    public void execute(UUID taskRunId) {
        MDC.put("taskRunId", taskRunId.toString());
        try {
            executeInternal(taskRunId);
        } finally {
            MDC.remove("taskRunId");
        }
    }

    private void executeInternal(UUID taskRunId) {
        String owner = workerIdentity.get();
        String lockKey = distributedLock.jobLockKey(taskRunId);

        // Exactly-once claim: a second worker cannot acquire a held job.
        if (!distributedLock.acquire(lockKey, owner, LOCK_TTL)) {
            return;
        }
        // Release only after the transaction completes, so the committed status
        // is visible before another worker could re-claim.
        registerLockRelease(lockKey, owner);

        TaskRun taskRun = taskRunRepository.findById(taskRunId).orElse(null);
        if (taskRun == null || taskRun.getStatus() != ExecutionStatus.QUEUED) {
            return; // already claimed/finished, or vanished
        }

        PipelineRun run = taskRun.getPipelineRun();
        startRunIfQueued(run);

        taskRun.setStatus(ExecutionStatus.RUNNING);
        taskRun.setWorkerId(owner);
        taskRun.setStartedAt(Instant.now());
        taskRunRepository.save(taskRun);

        try {
            taskRunner.run(taskRun.getTask());
            taskRun.setStatus(ExecutionStatus.SUCCESS);
            taskRun.setCompletedAt(Instant.now());
            taskRunRepository.save(taskRun);
            metricsService.taskSucceeded(Duration.between(taskRun.getStartedAt(), taskRun.getCompletedAt()));
            enqueueReadyChildren(run, taskRun.getTask().getId());
        } catch (RuntimeException ex) {
            log.warn("Task run {} failed: {}", taskRunId, ex.getMessage());
            ExecutionStatus outcome = retryService.handleFailure(taskRun, ex.getMessage());
            if (outcome == ExecutionStatus.RETRYING) {
                metricsService.taskRetried();
            } else {
                metricsService.taskFailed();
            }
        }

        finalizeRunIfComplete(run);
    }

    private void startRunIfQueued(PipelineRun run) {
        if (run.getStatus() == ExecutionStatus.QUEUED) {
            run.setStatus(ExecutionStatus.RUNNING);
            if (run.getStartedAt() == null) {
                run.setStartedAt(Instant.now());
            }
            pipelineRunRepository.save(run);
        }
    }

    /** Queues children of {@code completedTaskId} whose every parent has now succeeded. */
    private void enqueueReadyChildren(PipelineRun run, UUID completedTaskId) {
        UUID pipelineId = run.getPipeline().getId();
        List<PipelineDependency> edges = dependencyRepository.findByPipelineId(pipelineId);
        List<TaskRun> taskRuns = taskRunRepository.findByPipelineRunId(run.getId());

        Map<UUID, TaskRun> runByTaskId = new HashMap<>();
        for (TaskRun tr : taskRuns) {
            runByTaskId.put(tr.getTask().getId(), tr);
        }
        Map<UUID, List<UUID>> parentsByChild = new HashMap<>();
        for (PipelineDependency edge : edges) {
            parentsByChild.computeIfAbsent(edge.getChildTask().getId(), k -> new ArrayList<>())
                    .add(edge.getParentTask().getId());
        }

        for (PipelineDependency edge : edges) {
            if (!edge.getParentTask().getId().equals(completedTaskId)) {
                continue;
            }
            UUID childId = edge.getChildTask().getId();
            TaskRun childRun = runByTaskId.get(childId);
            if (childRun == null || childRun.getStatus() != ExecutionStatus.PENDING) {
                continue;
            }
            boolean allParentsSucceeded = parentsByChild.getOrDefault(childId, List.of()).stream()
                    .map(runByTaskId::get)
                    .allMatch(pr -> pr != null && pr.getStatus() == ExecutionStatus.SUCCESS);
            if (allParentsSucceeded) {
                childRun.setStatus(ExecutionStatus.QUEUED);
                taskRunRepository.save(childRun);
                queuePublisher.publishReady(childRun.getId());
            }
        }
    }

    private void finalizeRunIfComplete(PipelineRun run) {
        if (run.getStatus().isTerminal()) {
            return;
        }
        List<TaskRun> taskRuns = taskRunRepository.findByPipelineRunId(run.getId());
        boolean anyActive = taskRuns.stream().anyMatch(tr -> !isFinal(tr.getStatus()));
        if (anyActive) {
            return;
        }
        boolean allSucceeded = taskRuns.stream().allMatch(tr -> tr.getStatus() == ExecutionStatus.SUCCESS);
        if (allSucceeded) {
            transitionRun(run, ExecutionStatus.SUCCESS);
        } else if (taskRuns.stream().anyMatch(tr -> tr.getStatus() == ExecutionStatus.FAILED_PERMANENTLY)) {
            transitionRun(run, ExecutionStatus.FAILED);
            transitionRun(run, ExecutionStatus.FAILED_PERMANENTLY);
        }
        run.setCompletedAt(Instant.now());
        pipelineRunRepository.save(run);
    }

    private boolean isFinal(ExecutionStatus status) {
        return status == ExecutionStatus.SUCCESS
                || status == ExecutionStatus.FAILED_PERMANENTLY
                || status == ExecutionStatus.CANCELLED;
    }

    private void transitionRun(PipelineRun run, ExecutionStatus to) {
        if (stateMachine.canTransition(run.getStatus(), to)) {
            run.setStatus(to);
        }
    }

    private void registerLockRelease(String lockKey, String owner) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    distributedLock.release(lockKey, owner);
                }
            });
        } else {
            distributedLock.release(lockKey, owner);
        }
    }
}
