package com.pipeforge.pipeline.service;

import com.pipeforge.auth.repository.UserRepository;
import com.pipeforge.common.dto.PageResponse;
import com.pipeforge.exception.ResourceNotFoundException;
import com.pipeforge.pipeline.dto.CreatePipelineRequest;
import com.pipeforge.pipeline.dto.PipelineResponse;
import com.pipeforge.pipeline.dto.UpdatePipelineRequest;
import com.pipeforge.pipeline.entity.Pipeline;
import com.pipeforge.pipeline.entity.PipelineStatus;
import com.pipeforge.pipeline.entity.RetryPolicy;
import com.pipeforge.pipeline.mapper.PipelineMapper;
import com.pipeforge.pipeline.repository.PipelineRepository;
import com.pipeforge.scheduler.PipelineSchedulerService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Pipeline CRUD use-cases (App Flow §5.2–5.3, PRD §5.2). */
@Service
public class PipelineService {

    private final PipelineRepository pipelineRepository;
    private final UserRepository userRepository;
    private final PipelineMapper pipelineMapper;
    private final PipelineSchedulerService schedulerService;

    public PipelineService(PipelineRepository pipelineRepository,
                           UserRepository userRepository,
                           PipelineMapper pipelineMapper,
                           PipelineSchedulerService schedulerService) {
        this.pipelineRepository = pipelineRepository;
        this.userRepository = userRepository;
        this.pipelineMapper = pipelineMapper;
        this.schedulerService = schedulerService;
    }

    @Transactional
    public PipelineResponse create(CreatePipelineRequest request, UUID ownerId) {
        Pipeline pipeline = new Pipeline();
        pipeline.setName(request.name());
        pipeline.setDescription(request.description());
        pipeline.setScheduleCron(request.scheduleCron());
        pipeline.setRetryPolicy(request.retryPolicy() != null ? request.retryPolicy() : RetryPolicy.defaults());
        pipeline.setOwner(userRepository.getReferenceById(ownerId));
        pipeline.setStatus(PipelineStatus.DRAFT);
        pipeline.setVersion(1);

        Pipeline saved = pipelineRepository.save(pipeline);
        schedulerService.sync(saved);
        return pipelineMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<PipelineResponse> list(Pageable pageable) {
        return PageResponse.from(pipelineRepository.findAll(pageable), pipelineMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PipelineResponse get(UUID id) {
        return pipelineMapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public PipelineResponse update(UUID id, UpdatePipelineRequest request) {
        Pipeline pipeline = findOrThrow(id);
        pipelineMapper.updateEntity(request, pipeline);
        Pipeline saved = pipelineRepository.save(pipeline);
        schedulerService.sync(saved);
        return pipelineMapper.toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        Pipeline pipeline = findOrThrow(id);
        pipeline.setDeleted(true);
        pipelineRepository.save(pipeline);
        schedulerService.unschedule(id);
    }

    private Pipeline findOrThrow(UUID id) {
        return pipelineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pipeline not found: " + id));
    }
}
