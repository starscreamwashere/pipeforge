package com.pipeforge.worker.runtime;

import com.pipeforge.execution.queue.QueueKeys;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

/**
 * Consumes ready jobs from Redis via blocking {@code BRPOP} and dispatches each
 * to a virtual thread (Technical Requirements §8). Enabled by
 * {@code pipeforge.worker.consumer.enabled=true}.
 */
@Component
@ConditionalOnProperty(name = "pipeforge.worker.consumer.enabled", havingValue = "true")
public class ReadyQueueConsumer {

    private static final Logger log = LoggerFactory.getLogger(ReadyQueueConsumer.class);
    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(2);

    private final StringRedisTemplate redisTemplate;
    private final ExecutorService workerExecutor;
    private final JobExecutor jobExecutor;
    private final WorkerService workerService;

    private volatile boolean running = true;
    private Thread consumerThread;

    public ReadyQueueConsumer(StringRedisTemplate redisTemplate,
                              ExecutorService workerExecutor,
                              JobExecutor jobExecutor,
                              WorkerService workerService) {
        this.redisTemplate = redisTemplate;
        this.workerExecutor = workerExecutor;
        this.jobExecutor = jobExecutor;
        this.workerService = workerService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        workerService.registerSelf();
        consumerThread = new Thread(this::consumeLoop, "ready-queue-consumer");
        consumerThread.setDaemon(true);
        consumerThread.start();
        log.info("Ready-queue consumer started");
    }

    private void consumeLoop() {
        while (running) {
            try {
                String id = redisTemplate.opsForList().rightPop(QueueKeys.READY_QUEUE, POLL_TIMEOUT);
                workerService.heartbeat();
                if (id == null) {
                    continue;
                }
                UUID taskRunId = UUID.fromString(id);
                workerExecutor.submit(() -> runJob(taskRunId));
            } catch (RuntimeException ex) {
                log.warn("Ready-queue poll error: {}", ex.getMessage());
                sleepQuietly();
            }
        }
    }

    private void runJob(UUID taskRunId) {
        try {
            jobExecutor.execute(taskRunId);
            workerService.recordProcessed();
        } catch (RuntimeException ex) {
            log.error("Job {} crashed: {}", taskRunId, ex.getMessage());
        }
    }

    private void sleepQuietly() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (consumerThread != null) {
            consumerThread.interrupt();
        }
    }
}
