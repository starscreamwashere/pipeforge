package com.pipeforge.scheduler;

import com.pipeforge.pipeline.repository.PipelineRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Re-registers Quartz schedules for all active, cron-bearing pipelines on
 * startup (the in-memory job store does not survive restarts).
 */
@Component
public class PipelineScheduleInitializer {

    private static final Logger log = LoggerFactory.getLogger(PipelineScheduleInitializer.class);

    private final PipelineRepository pipelineRepository;
    private final PipelineSchedulerService schedulerService;

    public PipelineScheduleInitializer(PipelineRepository pipelineRepository,
                                       PipelineSchedulerService schedulerService) {
        this.pipelineRepository = pipelineRepository;
        this.schedulerService = schedulerService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerActiveSchedules() {
        pipelineRepository.findAll().forEach(pipeline -> {
            try {
                schedulerService.sync(pipeline);
            } catch (RuntimeException ex) {
                log.warn("Skipping schedule for pipeline {}: {}", pipeline.getId(), ex.getMessage());
            }
        });
    }
}
