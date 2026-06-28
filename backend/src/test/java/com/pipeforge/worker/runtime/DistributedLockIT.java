package com.pipeforge.worker.runtime;

import com.pipeforge.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Distributed-lock semantics (Phase 7.4 — "duplicate execution impossible"). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DistributedLockIT {

    @Autowired DistributedLock lock;

    private String key() {
        return "lock:test:" + UUID.randomUUID();
    }

    @Test
    void acquireIsExclusiveUntilReleased() {
        String key = key();
        assertThat(lock.acquire(key, "worker-a", Duration.ofSeconds(30))).isTrue();
        assertThat(lock.acquire(key, "worker-b", Duration.ofSeconds(30))).isFalse();

        lock.release(key, "worker-a");
        assertThat(lock.acquire(key, "worker-b", Duration.ofSeconds(30))).isTrue();
        lock.release(key, "worker-b");
    }

    @Test
    void releaseIgnoredWhenNotOwner() {
        String key = key();
        assertThat(lock.acquire(key, "worker-a", Duration.ofSeconds(30))).isTrue();

        lock.release(key, "worker-b"); // not the owner -> no-op
        assertThat(lock.acquire(key, "worker-c", Duration.ofSeconds(30))).isFalse(); // still held by a

        lock.release(key, "worker-a");
    }
}
