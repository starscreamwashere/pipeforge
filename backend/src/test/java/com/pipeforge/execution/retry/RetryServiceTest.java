package com.pipeforge.execution.retry;

import com.pipeforge.execution.entity.ExecutionStatus;
import com.pipeforge.execution.entity.PipelineRun;
import com.pipeforge.execution.entity.TaskRun;
import com.pipeforge.execution.queue.QueuePublisher;
import com.pipeforge.execution.repository.RetryRepository;
import com.pipeforge.execution.repository.TaskRunRepository;
import com.pipeforge.execution.state.ExecutionStateMachine;
import com.pipeforge.pipeline.entity.Pipeline;
import com.pipeforge.pipeline.entity.RetryPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RetryServiceTest {

    @Mock TaskRunRepository taskRunRepository;
    @Mock RetryRepository retryRepository;
    @Mock QueuePublisher queuePublisher;

    private RetryService retryService;

    @BeforeEach
    void setUp() {
        retryService = new RetryService(taskRunRepository, retryRepository, queuePublisher,
                new BackoffCalculator(), new ExecutionStateMachine());
    }

    private TaskRun runningTaskRun(int attempt, RetryPolicy policy) {
        Pipeline pipeline = new Pipeline();
        pipeline.setRetryPolicy(policy);
        PipelineRun run = new PipelineRun();
        run.setPipeline(pipeline);

        TaskRun taskRun = new TaskRun();
        taskRun.setId(UUID.randomUUID());
        taskRun.setPipelineRun(run);
        taskRun.setStatus(ExecutionStatus.RUNNING);
        taskRun.setAttempt(attempt);
        return taskRun;
    }

    @Test
    void schedulesRetryWhileAttemptsRemain() {
        TaskRun taskRun = runningTaskRun(0, new RetryPolicy(2, 5));

        ExecutionStatus result = retryService.handleFailure(taskRun, "boom");

        assertThat(result).isEqualTo(ExecutionStatus.RETRYING);
        assertThat(taskRun.getStatus()).isEqualTo(ExecutionStatus.RETRYING);
        assertThat(taskRun.getAttempt()).isEqualTo(1);
        assertThat(taskRun.getErrorMessage()).isEqualTo("boom");
        verify(retryRepository).save(any());
        verify(queuePublisher).publishRetry(eq(taskRun.getId()), anyLong());
    }

    @Test
    void deadLettersWhenRetriesExhausted() {
        TaskRun taskRun = runningTaskRun(2, new RetryPolicy(2, 5)); // attempt == maxRetries

        ExecutionStatus result = retryService.handleFailure(taskRun, "fatal");

        assertThat(result).isEqualTo(ExecutionStatus.FAILED_PERMANENTLY);
        assertThat(taskRun.getStatus()).isEqualTo(ExecutionStatus.FAILED_PERMANENTLY);
        assertThat(taskRun.getCompletedAt()).isNotNull();
        verify(queuePublisher, never()).publishRetry(any(), anyLong());
        verify(retryRepository, never()).save(any());
    }
}
