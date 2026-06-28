package com.pipeforge.execution.queue;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Publishes ready task-run IDs to the Redis ready queue (Technical Requirements §7).
 * Workers consume via {@code BRPOP} (Milestone 7).
 */
@Component
public class QueuePublisher {

    private final StringRedisTemplate redisTemplate;

    public QueuePublisher(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** LPUSH a ready task-run ID onto {@code queue:ready}. */
    public void publishReady(UUID taskRunId) {
        redisTemplate.opsForList().leftPush(QueueKeys.READY_QUEUE, taskRunId.toString());
    }
}
