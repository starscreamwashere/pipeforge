package com.pipeforge.worker.runtime;

import com.pipeforge.exception.JobExecutionException;
import com.pipeforge.pipeline.entity.PipelineTask;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Executes the actual work of a task. PipeForge V1 simulates execution (it is an
 * orchestration platform, not a compute engine). A task's {@code config_payload}
 * may set {@code "fail": true} to deterministically simulate a failure — used to
 * exercise the retry / dead-letter paths.
 */
@Component
public class TaskRunner {

    public void run(PipelineTask task) {
        Map<String, Object> config = task.getConfigPayload();
        if (config != null && isFlagSet(config.get("fail"))) {
            throw new JobExecutionException("Simulated failure for task " + task.getTaskName());
        }
        // Success: no-op. Real connectors would run here.
    }

    private boolean isFlagSet(Object value) {
        return value instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(value));
    }
}
