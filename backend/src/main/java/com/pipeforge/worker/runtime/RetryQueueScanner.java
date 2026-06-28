package com.pipeforge.worker.runtime;

import com.pipeforge.execution.entity.ExecutionStatus;
import com.pipeforge.execution.queue.QueueKeys;
import com.pipeforge.execution.queue.QueuePublisher;
import com.pipeforge.execution.repository.TaskRunRepository;
import com.pipeforge.execution.state.ExecutionStateMachine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Moves due retries from the Redis retry sorted-set back onto the ready queue
 * (Technical Requirements §7). Each entry is claimed atomically via {@code ZREM},
 * so it is re-enqueued exactly once even with multiple workers.
 */
@Component
@ConditionalOnProperty(name = "pipeforge.worker.consumer.enabled", havingValue = "true")
public class RetryQueueScanner {

    private final StringRedisTemplate redisTemplate;
    private final TaskRunRepository taskRunRepository;
    private final QueuePublisher queuePublisher;
    private final ExecutionStateMachine stateMachine;

    public RetryQueueScanner(StringRedisTemplate redisTemplate,
                             TaskRunRepository taskRunRepository,
                             QueuePublisher queuePublisher,
                             ExecutionStateMachine stateMachine) {
        this.redisTemplate = redisTemplate;
        this.taskRunRepository = taskRunRepository;
        this.queuePublisher = queuePublisher;
        this.stateMachine = stateMachine;
    }

    @Scheduled(fixedDelayString = "1000")
    @Transactional
    public void scan() {
        long now = Instant.now().getEpochSecond();
        Set<String> due = redisTemplate.opsForZSet().rangeByScore(QueueKeys.RETRY_QUEUE, 0, now);
        if (due == null) {
            return;
        }
        for (String id : due) {
            Long removed = redisTemplate.opsForZSet().remove(QueueKeys.RETRY_QUEUE, id);
            if (removed == null || removed == 0) {
                continue; // claimed by another scanner
            }
            requeue(UUID.fromString(id));
        }
    }

    private void requeue(UUID taskRunId) {
        taskRunRepository.findById(taskRunId).ifPresent(taskRun -> {
            if (taskRun.getStatus() == ExecutionStatus.RETRYING
                    && stateMachine.canTransition(ExecutionStatus.RETRYING, ExecutionStatus.QUEUED)) {
                taskRun.setStatus(ExecutionStatus.QUEUED);
                taskRunRepository.save(taskRun);
                queuePublisher.publishReady(taskRunId);
            }
        });
    }
}
