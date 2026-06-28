package com.pipeforge.pipeline.repository;

import com.pipeforge.pipeline.entity.Pipeline;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PipelineRepository extends JpaRepository<Pipeline, UUID> {

    Page<Pipeline> findByOwnerId(UUID ownerId, Pageable pageable);
}
