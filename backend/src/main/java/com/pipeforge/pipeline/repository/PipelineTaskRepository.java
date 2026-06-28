package com.pipeforge.pipeline.repository;

import com.pipeforge.pipeline.entity.PipelineTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PipelineTaskRepository extends JpaRepository<PipelineTask, UUID> {

    List<PipelineTask> findByPipelineId(UUID pipelineId);

    long countByPipelineId(UUID pipelineId);
}
