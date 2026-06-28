package com.pipeforge.execution.retry;

import com.pipeforge.execution.entity.ExecutionStatus;
import com.pipeforge.execution.entity.Retry;
import com.pipeforge.execution.entity.TaskRun;
import com.pipeforge.execution.queue.QueuePublisher;
import com.pipeforge.execution.repository.RetryRepository;
import com.pipeforge.execution.repository.TaskRunRepository;
import com.pipeforge.execution.state.ExecutionStateMachine;
import com.pipeforge.pipeline.entity.RetryPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Handles task-run failures (PRD §5.7, App Flow §5.8): schedules an
 * exponential-backoff retry while attempts remain, otherwise moves the task to
 * the dead-letter state ({@code FAILED_PERMANENTLY}).
 */
@Service
public class RetryService {

    private final TaskRunRepository taskRunRepository;
    private final RetryRepository retryRepository;
    private final QueuePublisher queuePublisher;
    private final BackoffCalculator backoffCalculator;
    private final ExecutionStateMachine stateMachine;

    public RetryService(TaskRunRepository taskRunRepository,
                        RetryRepository retryRepository,
                        QueuePublisher queuePublisher,
                        BackoffCalculator backoffCalculator,
                        ExecutionStateMachine stateMachine) {
        this.taskRunRepository = taskRunRepository;
        this.retryRepository = retryRepository;
        this.queuePublisher = queuePublisher;
        this.backoffCalculator = backoffCalculator;
        this.stateMachine = stateMachine;
    }

    /**
     * Records a task-run failure and either schedules a delayed retry or
     * dead-letters the task. Returns the resulting status.
     */
    @Transactional
    public ExecutionStatus handleFailure(TaskRun taskRun, String reason) {
        RetryPolicy policy = resolvePolicy(taskRun);
        taskRun.setErrorMessage(reason);

        if (taskRun.getAttempt() < policy.maxRetries()) {
            long delaySeconds = backoffCalculator.computeDelaySeconds(taskRun.getAttempt(), policy.backoffSeconds());
            Instant scheduledAt = Instant.now().plusSeconds(delaySeconds);
            int nextAttempt = taskRun.getAttempt() + 1;

            taskRun.setAttempt(nextAttempt);
            transition(taskRun, ExecutionStatus.RETRYING);
            taskRunRepository.save(taskRun);

            Retry retry = new Retry();
            retry.setTaskRun(taskRun);
            retry.setAttempt(nextAttempt);
            retry.setScheduledAt(scheduledAt);
            retry.setReason(reason);
            retryRepository.save(retry);

            queuePublisher.publishRetry(taskRun.getId(), scheduledAt.getEpochSecond());
            return ExecutionStatus.RETRYING;
        }

        // Retries exhausted -> dead-letter.
        transition(taskRun, ExecutionStatus.FAILED);
        transition(taskRun, ExecutionStatus.FAILED_PERMANENTLY);
        taskRun.setCompletedAt(Instant.now());
        taskRunRepository.save(taskRun);
        return ExecutionStatus.FAILED_PERMANENTLY;
    }

    private RetryPolicy resolvePolicy(TaskRun taskRun) {
        RetryPolicy policy = taskRun.getPipelineRun().getPipeline().getRetryPolicy();
        return policy != null ? policy : RetryPolicy.defaults();
    }

    private void transition(TaskRun taskRun, ExecutionStatus to) {
        stateMachine.assertCanTransition(taskRun.getStatus(), to);
        taskRun.setStatus(to);
    }
}
