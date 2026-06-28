package com.pipeforge.scheduler;

import com.pipeforge.exception.ValidationException;
import com.pipeforge.pipeline.entity.Pipeline;
import com.pipeforge.pipeline.entity.PipelineStatus;
import org.quartz.CronExpression;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * Registers/unregisters Quartz cron triggers for pipelines (PRD §5.5).
 *
 * <p>A pipeline is scheduled only when it is {@code ACTIVE} and has a valid
 * cron expression; otherwise any existing schedule is removed.
 */
@Service
public class PipelineSchedulerService {

    private static final String GROUP = "pipelines";

    private final Scheduler scheduler;

    public PipelineSchedulerService(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    /** Schedules the pipeline if it is ACTIVE with a cron, else unschedules it. */
    public void sync(Pipeline pipeline) {
        boolean schedulable = pipeline.getStatus() == PipelineStatus.ACTIVE
                && StringUtils.hasText(pipeline.getScheduleCron());
        if (!schedulable) {
            unschedule(pipeline.getId());
            return;
        }

        String cron = pipeline.getScheduleCron();
        if (!CronExpression.isValidExpression(cron)) {
            throw new ValidationException("Invalid cron expression: " + cron);
        }

        try {
            JobKey jobKey = jobKey(pipeline.getId());
            JobDetail job = JobBuilder.newJob(PipelineTriggerJob.class)
                    .withIdentity(jobKey)
                    .usingJobData(PipelineTriggerJob.PIPELINE_ID, pipeline.getId().toString())
                    .build();
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger-" + pipeline.getId(), GROUP)
                    .forJob(jobKey)
                    .withSchedule(CronScheduleBuilder.cronSchedule(cron)
                            .withMisfireHandlingInstructionDoNothing())
                    .build();

            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
            }
            scheduler.scheduleJob(job, trigger);
        } catch (SchedulerException ex) {
            throw new IllegalStateException("Failed to schedule pipeline " + pipeline.getId(), ex);
        }
    }

    public void unschedule(UUID pipelineId) {
        try {
            scheduler.deleteJob(jobKey(pipelineId));
        } catch (SchedulerException ex) {
            throw new IllegalStateException("Failed to unschedule pipeline " + pipelineId, ex);
        }
    }

    public boolean isScheduled(UUID pipelineId) {
        try {
            return scheduler.checkExists(jobKey(pipelineId));
        } catch (SchedulerException ex) {
            throw new IllegalStateException("Failed to query schedule for pipeline " + pipelineId, ex);
        }
    }

    private JobKey jobKey(UUID pipelineId) {
        return JobKey.jobKey("pipeline-" + pipelineId, GROUP);
    }
}
