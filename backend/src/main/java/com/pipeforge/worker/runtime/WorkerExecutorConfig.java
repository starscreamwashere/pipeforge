package com.pipeforge.worker.runtime;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Virtual-thread executor for running jobs (Technical Requirements §8 —
 * {@code Executors.newVirtualThreadPerTaskExecutor()}). Lightweight concurrency
 * suited to the I/O-heavy nature of pipeline tasks.
 */
@Configuration
public class WorkerExecutorConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService workerExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
