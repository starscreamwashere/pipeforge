package com.pipeforge.worker.runtime;

import com.pipeforge.worker.entity.Worker;
import com.pipeforge.worker.entity.WorkerStatus;
import com.pipeforge.worker.repository.WorkerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** Registers and maintains this worker instance's row for observability (App Flow §5.9). */
@Service
public class WorkerService {

    private final WorkerRepository workerRepository;
    private final WorkerIdentity identity;

    public WorkerService(WorkerRepository workerRepository, WorkerIdentity identity) {
        this.workerRepository = workerRepository;
        this.identity = identity;
    }

    @Transactional
    public void registerSelf() {
        Worker worker = workerRepository.findByName(identity.get()).orElseGet(Worker::new);
        worker.setName(identity.get());
        worker.setStatus(WorkerStatus.ACTIVE);
        worker.setLastHeartbeatAt(Instant.now());
        workerRepository.save(worker);
    }

    @Transactional
    public void heartbeat() {
        workerRepository.findByName(identity.get()).ifPresent(worker -> {
            worker.setLastHeartbeatAt(Instant.now());
            workerRepository.save(worker);
        });
    }

    @Transactional
    public void recordProcessed() {
        workerRepository.findByName(identity.get()).ifPresent(worker -> {
            worker.setJobsProcessed(worker.getJobsProcessed() + 1);
            worker.setCurrentTaskRunId(null);
            worker.setStatus(WorkerStatus.ACTIVE);
            worker.setLastHeartbeatAt(Instant.now());
            workerRepository.save(worker);
        });
    }
}
