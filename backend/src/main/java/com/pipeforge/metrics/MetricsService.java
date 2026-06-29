package com.pipeforge.metrics;

import com.pipeforge.execution.entity.ExecutionStatus;
import com.pipeforge.execution.queue.QueueKeys;
import com.pipeforge.execution.repository.PipelineRunRepository;
import com.pipeforge.pipeline.repository.PipelineRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Application metrics (Technical Requirements §13). Counters/timer are registered
 * eagerly so they appear in {@code /actuator/prometheus} from startup; gauges are
 * sampled live from the database and Redis on each scrape.
 */
@Service
public class MetricsService {

    private final Counter executionsTriggered;
    private final Counter tasksSucceeded;
    private final Counter tasksFailed;
    private final Counter tasksRetried;
    private final Timer taskDuration;

    public MetricsService(MeterRegistry registry,
                          PipelineRepository pipelineRepository,
                          PipelineRunRepository pipelineRunRepository,
                          StringRedisTemplate redisTemplate) {

        this.executionsTriggered = Counter.builder("pipeforge.executions.triggered")
                .description("Total pipeline executions triggered").register(registry);
        this.tasksSucceeded = Counter.builder("pipeforge.tasks.succeeded")
                .description("Total task runs that succeeded").register(registry);
        this.tasksFailed = Counter.builder("pipeforge.tasks.failed")
                .description("Total task runs that failed permanently").register(registry);
        this.tasksRetried = Counter.builder("pipeforge.tasks.retried")
                .description("Total task-run retries scheduled").register(registry);
        this.taskDuration = Timer.builder("pipeforge.task.duration")
                .description("Task execution latency").register(registry);

        Gauge.builder("pipeforge.pipelines.count", pipelineRepository, r -> (double) r.count())
                .description("Total (non-deleted) pipelines").register(registry);
        Gauge.builder("pipeforge.executions.active", pipelineRunRepository,
                        r -> (double) r.countByStatus(ExecutionStatus.RUNNING))
                .description("Currently running executions").register(registry);
        Gauge.builder("pipeforge.queue.ready.depth", redisTemplate, MetricsService::readyDepth)
                .description("Depth of the Redis ready queue").register(registry);
    }

    public void executionTriggered() {
        executionsTriggered.increment();
    }

    public void taskSucceeded(Duration duration) {
        tasksSucceeded.increment();
        taskDuration.record(duration);
    }

    public void taskFailed() {
        tasksFailed.increment();
    }

    public void taskRetried() {
        tasksRetried.increment();
    }

    private static double readyDepth(StringRedisTemplate redisTemplate) {
        Long size = redisTemplate.opsForList().size(QueueKeys.READY_QUEUE);
        return size == null ? 0d : size.doubleValue();
    }
}
