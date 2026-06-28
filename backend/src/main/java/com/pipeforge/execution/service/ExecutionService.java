package com.pipeforge.execution.service;

import com.pipeforge.common.dto.PageResponse;
import com.pipeforge.exception.ResourceNotFoundException;
import com.pipeforge.exception.ValidationException;
import com.pipeforge.execution.dto.ExecutionDetailResponse;
import com.pipeforge.execution.dto.ExecutionResponse;
import com.pipeforge.execution.dto.TaskRunResponse;
import com.pipeforge.execution.entity.ExecutionStatus;
import com.pipeforge.execution.entity.PipelineRun;
import com.pipeforge.execution.entity.TaskRun;
import com.pipeforge.execution.entity.TriggerType;
import com.pipeforge.execution.mapper.ExecutionMapper;
import com.pipeforge.execution.mapper.TaskRunMapper;
import com.pipeforge.execution.queue.QueuePublisher;
import com.pipeforge.execution.repository.PipelineRunRepository;
import com.pipeforge.execution.repository.TaskRunRepository;
import com.pipeforge.execution.state.ExecutionStateMachine;
import com.pipeforge.pipeline.dag.DagAlgorithms;
import com.pipeforge.pipeline.entity.Pipeline;
import com.pipeforge.pipeline.entity.PipelineDependency;
import com.pipeforge.pipeline.entity.PipelineTask;
import com.pipeforge.pipeline.repository.PipelineDependencyRepository;
import com.pipeforge.pipeline.repository.PipelineRepository;
import com.pipeforge.pipeline.repository.PipelineTaskRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Drives pipeline executions (PRD §5.4, App Flow §5.5):
 * trigger a run, persist it and its task runs, and enqueue the root tasks
 * (those with no dependencies) onto the Redis ready queue.
 */
@Service
public class ExecutionService {

    private final PipelineRepository pipelineRepository;
    private final PipelineTaskRepository taskRepository;
    private final PipelineDependencyRepository dependencyRepository;
    private final PipelineRunRepository pipelineRunRepository;
    private final TaskRunRepository taskRunRepository;
    private final ExecutionStateMachine stateMachine;
    private final QueuePublisher queuePublisher;
    private final ExecutionMapper executionMapper;
    private final TaskRunMapper taskRunMapper;

    public ExecutionService(PipelineRepository pipelineRepository,
                            PipelineTaskRepository taskRepository,
                            PipelineDependencyRepository dependencyRepository,
                            PipelineRunRepository pipelineRunRepository,
                            TaskRunRepository taskRunRepository,
                            ExecutionStateMachine stateMachine,
                            QueuePublisher queuePublisher,
                            ExecutionMapper executionMapper,
                            TaskRunMapper taskRunMapper) {
        this.pipelineRepository = pipelineRepository;
        this.taskRepository = taskRepository;
        this.dependencyRepository = dependencyRepository;
        this.pipelineRunRepository = pipelineRunRepository;
        this.taskRunRepository = taskRunRepository;
        this.stateMachine = stateMachine;
        this.queuePublisher = queuePublisher;
        this.executionMapper = executionMapper;
        this.taskRunMapper = taskRunMapper;
    }

    @Transactional
    public ExecutionResponse trigger(UUID pipelineId, TriggerType triggerType) {
        Pipeline pipeline = pipelineRepository.findById(pipelineId)
                .orElseThrow(() -> new ResourceNotFoundException("Pipeline not found: " + pipelineId));

        List<PipelineTask> tasks = taskRepository.findByPipelineId(pipelineId);
        if (tasks.isEmpty()) {
            throw new ValidationException("Pipeline has no tasks to execute");
        }
        List<PipelineDependency> edges = dependencyRepository.findByPipelineId(pipelineId);
        Map<UUID, Integer> indegree = indegrees(tasks, edges);

        // Create the run and move PENDING -> QUEUED through the state machine.
        PipelineRun run = new PipelineRun();
        run.setPipeline(pipeline);
        run.setTriggerType(triggerType == null ? TriggerType.MANUAL : triggerType);
        run.setStatus(ExecutionStatus.PENDING);
        transition(run, ExecutionStatus.QUEUED);
        run.setStartedAt(Instant.now());
        pipelineRunRepository.save(run);

        // One task run per task, all PENDING.
        Map<UUID, TaskRun> taskRunByTaskId = new HashMap<>();
        for (PipelineTask task : tasks) {
            TaskRun taskRun = new TaskRun();
            taskRun.setPipelineRun(run);
            taskRun.setTask(task);
            taskRun.setStatus(ExecutionStatus.PENDING);
            taskRunByTaskId.put(task.getId(), taskRunRepository.save(taskRun));
        }

        // Enqueue the roots (indegree 0) — the only tasks eligible to run immediately.
        for (PipelineTask task : tasks) {
            if (indegree.get(task.getId()) == 0) {
                TaskRun rootRun = taskRunByTaskId.get(task.getId());
                transition(rootRun, ExecutionStatus.QUEUED);
                taskRunRepository.save(rootRun);
                queuePublisher.publishReady(rootRun.getId());
            }
        }

        return executionMapper.toResponse(run);
    }

    @Transactional(readOnly = true)
    public PageResponse<ExecutionResponse> list(Pageable pageable) {
        return PageResponse.from(
                pipelineRunRepository.findAllByOrderByCreatedAtDesc(pageable), executionMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ExecutionResponse get(UUID runId) {
        return executionMapper.toResponse(requireRun(runId));
    }

    @Transactional(readOnly = true)
    public ExecutionDetailResponse getDetail(UUID runId) {
        PipelineRun run = requireRun(runId);
        List<TaskRunResponse> taskRuns = taskRunRepository.findByPipelineRunId(runId).stream()
                .map(taskRunMapper::toResponse).toList();
        return new ExecutionDetailResponse(executionMapper.toResponse(run), taskRuns);
    }

    @Transactional
    public ExecutionResponse cancel(UUID runId) {
        PipelineRun run = requireRun(runId);
        transition(run, ExecutionStatus.CANCELLED);
        run.setCompletedAt(Instant.now());

        // Cancel any not-yet-terminal task runs.
        for (TaskRun taskRun : taskRunRepository.findByPipelineRunId(runId)) {
            if (stateMachine.canTransition(taskRun.getStatus(), ExecutionStatus.CANCELLED)) {
                taskRun.setStatus(ExecutionStatus.CANCELLED);
                taskRunRepository.save(taskRun);
            }
        }
        return executionMapper.toResponse(pipelineRunRepository.save(run));
    }

    private void transition(PipelineRun run, ExecutionStatus to) {
        stateMachine.assertCanTransition(run.getStatus(), to);
        run.setStatus(to);
    }

    private void transition(TaskRun taskRun, ExecutionStatus to) {
        stateMachine.assertCanTransition(taskRun.getStatus(), to);
        taskRun.setStatus(to);
    }

    private Map<UUID, Integer> indegrees(List<PipelineTask> tasks, List<PipelineDependency> edges) {
        List<UUID> ids = tasks.stream().map(PipelineTask::getId).toList();
        Map<UUID, List<UUID>> adjacency = new HashMap<>();
        Map<UUID, Integer> indegree = new HashMap<>();
        for (UUID id : ids) {
            adjacency.put(id, new ArrayList<>());
            indegree.put(id, 0);
        }
        for (PipelineDependency edge : edges) {
            adjacency.get(edge.getParentTask().getId()).add(edge.getChildTask().getId());
            indegree.merge(edge.getChildTask().getId(), 1, Integer::sum);
        }
        // Defensive: a stored DAG should already be acyclic.
        DagAlgorithms.assertAcyclic(ids, adjacency);
        return indegree;
    }

    private PipelineRun requireRun(UUID runId) {
        return pipelineRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Execution not found: " + runId));
    }
}
