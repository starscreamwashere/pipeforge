package com.pipeforge.execution.repository;

import com.pipeforge.execution.entity.TaskRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskRunRepository extends JpaRepository<TaskRun, UUID> {

    List<TaskRun> findByPipelineRunId(UUID pipelineRunId);
}
