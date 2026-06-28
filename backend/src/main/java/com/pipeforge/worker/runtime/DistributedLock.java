package com.pipeforge.worker.runtime;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * Redis-backed distributed lock (Technical Requirements §7/§8): {@code SETNX}
 * with a TTL guarantees exactly-once job claiming — a second worker cannot
 * acquire a job already held, and the TTL prevents a crashed worker from
 * deadlocking the job.
 */
@Component
public class DistributedLock {

    private final StringRedisTemplate redisTemplate;

    public DistributedLock(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean acquire(String key, String owner, Duration ttl) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, owner, ttl));
    }

    /** Releases the lock only if still owned by {@code owner}. */
    public void release(String key, String owner) {
        if (owner.equals(redisTemplate.opsForValue().get(key))) {
            redisTemplate.delete(key);
        }
    }

    public String jobLockKey(UUID taskRunId) {
        return "lock:job:" + taskRunId;
    }
}
