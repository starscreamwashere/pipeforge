package com.pipeforge.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Fixed-window rate limiter backed by Redis (Technical Requirements §3/§7).
 * The first request in a window sets the key TTL; subsequent requests increment
 * the counter until it expires.
 */
@Component
public class RateLimiter {

    private final StringRedisTemplate redisTemplate;

    public RateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** Returns {@code true} if the request is allowed (within the limit). */
    public boolean allow(String key, int limit, Duration window) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, window);
        }
        return count == null || count <= limit;
    }
}
