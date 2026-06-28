package com.pipeforge.worker.repository;

import com.pipeforge.worker.entity.Worker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkerRepository extends JpaRepository<Worker, UUID> {

    Optional<Worker> findByName(String name);

    List<Worker> findAllByOrderByCreatedAtAsc();
}
