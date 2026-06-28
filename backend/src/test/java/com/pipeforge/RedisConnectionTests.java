package com.pipeforge;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the Redis runtime store is reachable and the configured serializers
 * round-trip values (Phase 1.4 — "Redis connection works"). Runs against a real
 * Redis via Testcontainers.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RedisConnectionTests {

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @Test
    void connectionPingSucceeds() {
        RedisConnectionFactory factory = redisTemplate.getConnectionFactory();
        assertThat(factory).isNotNull();
        try (var connection = factory.getConnection()) {
            assertThat(connection.ping()).isEqualToIgnoringCase("PONG");
        }
    }

    @Test
    void valueRoundTripsThroughConfiguredSerializers() {
        String key = "pipeforge:test:greeting";
        redisTemplate.opsForValue().set(key, "hello");

        assertThat(redisTemplate.opsForValue().get(key)).isEqualTo("hello");

        redisTemplate.delete(key);
    }
}
