package com.pipeforge.pipeline.repository;

import com.pipeforge.pipeline.entity.PipelineDependency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PipelineDependencyRepository extends JpaRepository<PipelineDependency, UUID> {

    List<PipelineDependency> findByPipelineId(UUID pipelineId);

    boolean existsByPipelineIdAndParentTaskIdAndChildTaskId(UUID pipelineId, UUID parentTaskId, UUID childTaskId);
}
