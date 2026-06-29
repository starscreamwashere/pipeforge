package com.pipeforge.worker.service;

import com.pipeforge.exception.ResourceNotFoundException;
import com.pipeforge.worker.dto.WorkerResponse;
import com.pipeforge.worker.mapper.WorkerMapper;
import com.pipeforge.worker.repository.WorkerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Read-only worker queries for monitoring (App Flow §5.9). */
@Service
public class WorkerQueryService {

    private final WorkerRepository workerRepository;
    private final WorkerMapper workerMapper;

    public WorkerQueryService(WorkerRepository workerRepository, WorkerMapper workerMapper) {
        this.workerRepository = workerRepository;
        this.workerMapper = workerMapper;
    }

    @Transactional(readOnly = true)
    public List<WorkerResponse> list() {
        return workerRepository.findAllByOrderByCreatedAtAsc().stream().map(workerMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public WorkerResponse get(UUID id) {
        return workerRepository.findById(id).map(workerMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found: " + id));
    }
}
