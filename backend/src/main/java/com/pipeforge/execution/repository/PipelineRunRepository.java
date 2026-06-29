package com.pipeforge.execution.repository;

import com.pipeforge.execution.entity.ExecutionStatus;
import com.pipeforge.execution.entity.PipelineRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PipelineRunRepository extends JpaRepository<PipelineRun, UUID> {

    Page<PipelineRun> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<PipelineRun> findByPipelineId(UUID pipelineId, Pageable pageable);

    long countByStatus(ExecutionStatus status);
}
