package com.pipeforge.scheduler;

import com.pipeforge.execution.entity.TriggerType;
import com.pipeforge.execution.service.ExecutionService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

/**
 * Quartz job that fires a CRON-triggered execution for a scheduled pipeline
 * (App Flow §5.5). Field injection is required because Quartz instantiates the
 * job and Spring's {@code AutowireCapableBeanJobFactory} autowires it.
 */
public class PipelineTriggerJob implements Job {

    public static final String PIPELINE_ID = "pipelineId";

    private static final Logger log = LoggerFactory.getLogger(PipelineTriggerJob.class);

    @Autowired
    private ExecutionService executionService;

    @Override
    public void execute(JobExecutionContext context) {
        UUID pipelineId = UUID.fromString(context.getMergedJobDataMap().getString(PIPELINE_ID));
        try {
            executionService.trigger(pipelineId, TriggerType.CRON);
        } catch (RuntimeException ex) {
            // e.g. the pipeline has no tasks — log and let the next fire retry rather
            // than spamming Quartz misfires.
            log.warn("Scheduled trigger for pipeline {} failed: {}", pipelineId, ex.getMessage());
        }
    }
}
