package com.pipeforge.execution.repository;

import com.pipeforge.execution.entity.Retry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RetryRepository extends JpaRepository<Retry, UUID> {

    List<Retry> findByTaskRunIdOrderByAttemptAsc(UUID taskRunId);
}
