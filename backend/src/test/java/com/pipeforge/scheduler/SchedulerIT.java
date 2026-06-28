package com.pipeforge.scheduler;

import com.pipeforge.TestcontainersConfiguration;
import com.pipeforge.auth.entity.Role;
import com.pipeforge.auth.entity.User;
import com.pipeforge.auth.repository.UserRepository;
import com.pipeforge.execution.entity.TriggerType;
import com.pipeforge.execution.repository.PipelineRunRepository;
import com.pipeforge.pipeline.entity.Pipeline;
import com.pipeforge.pipeline.entity.PipelineStatus;
import com.pipeforge.pipeline.entity.PipelineTask;
import com.pipeforge.pipeline.entity.TaskType;
import com.pipeforge.pipeline.repository.PipelineRepository;
import com.pipeforge.pipeline.repository.PipelineTaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies Quartz cron scheduling actually fires an execution
 * (Phase 6.2 — "cron triggers execution").
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SchedulerIT {

    @Autowired UserRepository userRepository;
    @Autowired PipelineRepository pipelineRepository;
    @Autowired PipelineTaskRepository taskRepository;
    @Autowired PipelineRunRepository pipelineRunRepository;
    @Autowired PipelineSchedulerService schedulerService;

    @Test
    void cronScheduleTriggersAnExecution() throws InterruptedException {
        User owner = new User();
        owner.setName("Sched");
        owner.setEmail("sched-" + System.nanoTime() + "@pipeforge.dev");
        owner.setPasswordHash("hash");
        owner.setRole(Role.ENGINEER);
        userRepository.save(owner);

        Pipeline pipeline = new Pipeline();
        pipeline.setName("cron-pipe");
        pipeline.setOwner(owner);
        pipeline.setStatus(PipelineStatus.ACTIVE);
        pipeline.setScheduleCron("0/1 * * * * ?"); // every second
        pipelineRepository.save(pipeline);

        PipelineTask task = new PipelineTask();
        task.setPipeline(pipeline);
        task.setTaskName("extract");
        task.setTaskType(TaskType.EXTRACT);
        taskRepository.save(task);

        try {
            schedulerService.sync(pipeline);
            assertThat(schedulerService.isScheduled(pipeline.getId())).isTrue();

            boolean fired = false;
            Instant deadline = Instant.now().plus(Duration.ofSeconds(8));
            while (Instant.now().isBefore(deadline)) {
                long runs = pipelineRunRepository.findByPipelineId(pipeline.getId(), PageRequest.of(0, 5))
                        .getTotalElements();
                if (runs > 0) {
                    fired = true;
                    break;
                }
                Thread.sleep(250);
            }

            assertThat(fired).as("a cron-triggered run should appear within 8s").isTrue();
            assertThat(pipelineRunRepository.findByPipelineId(pipeline.getId(), PageRequest.of(0, 5))
                    .getContent().getFirst().getTriggerType()).isEqualTo(TriggerType.CRON);
        } finally {
            schedulerService.unschedule(pipeline.getId());
        }
    }
}
