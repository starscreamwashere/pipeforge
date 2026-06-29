package com.pipeforge.pipeline.repository;

import com.pipeforge.pipeline.entity.Pipeline;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PipelineRepository extends JpaRepository<Pipeline, UUID> {

    // Eagerly fetch the owner so list rendering (ownerName) avoids an N+1 query.
    @Override
    @EntityGraph(attributePaths = "owner")
    Page<Pipeline> findAll(Pageable pageable);

    Page<Pipeline> findByOwnerId(UUID ownerId, Pageable pageable);
}
