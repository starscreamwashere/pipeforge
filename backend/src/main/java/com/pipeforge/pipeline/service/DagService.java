package com.pipeforge.pipeline.service;

import com.pipeforge.exception.ResourceNotFoundException;
import com.pipeforge.exception.ValidationException;
import com.pipeforge.pipeline.dag.DagAlgorithms;
import com.pipeforge.pipeline.dto.CreateDependencyRequest;
import com.pipeforge.pipeline.dto.CreateTaskRequest;
import com.pipeforge.pipeline.dto.DagResponse;
import com.pipeforge.pipeline.dto.DependencyResponse;
import com.pipeforge.pipeline.dto.TaskResponse;
import com.pipeforge.pipeline.entity.Pipeline;
import com.pipeforge.pipeline.entity.PipelineDependency;
import com.pipeforge.pipeline.entity.PipelineTask;
import com.pipeforge.pipeline.mapper.DependencyMapper;
import com.pipeforge.pipeline.mapper.TaskMapper;
import com.pipeforge.pipeline.repository.PipelineDependencyRepository;
import com.pipeforge.pipeline.repository.PipelineRepository;
import com.pipeforge.pipeline.repository.PipelineTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DAG construction and validation (PRD §5.3, App Flow §5.3 step 3).
 *
 * <p>Adding a dependency is rejected if it would create a cycle, so a
 * pipeline's dependency graph is always a DAG.
 */
@Service
public class DagService {

    private final PipelineRepository pipelineRepository;
    private final PipelineTaskRepository taskRepository;
    private final PipelineDependencyRepository dependencyRepository;
    private final TaskMapper taskMapper;
    private final DependencyMapper dependencyMapper;

    public DagService(PipelineRepository pipelineRepository,
                      PipelineTaskRepository taskRepository,
                      PipelineDependencyRepository dependencyRepository,
                      TaskMapper taskMapper,
                      DependencyMapper dependencyMapper) {
        this.pipelineRepository = pipelineRepository;
        this.taskRepository = taskRepository;
        this.dependencyRepository = dependencyRepository;
        this.taskMapper = taskMapper;
        this.dependencyMapper = dependencyMapper;
    }

    @Transactional
    public TaskResponse addTask(UUID pipelineId, CreateTaskRequest request) {
        Pipeline pipeline = requirePipeline(pipelineId);

        PipelineTask task = new PipelineTask();
        task.setPipeline(pipeline);
        task.setTaskName(request.taskName());
        task.setTaskType(request.taskType());
        task.setConfigPayload(request.configPayload());
        task.setTimeoutSeconds(request.timeoutSeconds());
        task.setExecutionOrderHint(request.executionOrderHint());

        return taskMapper.toResponse(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> listTasks(UUID pipelineId) {
        requirePipeline(pipelineId);
        return taskRepository.findByPipelineId(pipelineId).stream().map(taskMapper::toResponse).toList();
    }

    @Transactional
    public void deleteTask(UUID pipelineId, UUID taskId) {
        PipelineTask task = requireTaskInPipeline(pipelineId, taskId);
        taskRepository.delete(task);
    }

    @Transactional
    public DependencyResponse addDependency(UUID pipelineId, CreateDependencyRequest request) {
        requirePipeline(pipelineId);

        if (request.parentTaskId().equals(request.childTaskId())) {
            throw new ValidationException("A task cannot depend on itself");
        }
        PipelineTask parent = requireTaskInPipeline(pipelineId, request.parentTaskId());
        PipelineTask child = requireTaskInPipeline(pipelineId, request.childTaskId());

        if (dependencyRepository.existsByPipelineIdAndParentTaskIdAndChildTaskId(
                pipelineId, parent.getId(), child.getId())) {
            throw new ValidationException("Dependency already exists");
        }

        // Reject the edge if it would introduce a cycle.
        List<PipelineTask> tasks = taskRepository.findByPipelineId(pipelineId);
        List<PipelineDependency> edges = dependencyRepository.findByPipelineId(pipelineId);
        Map<UUID, List<UUID>> adjacency = buildAdjacency(tasks, edges);
        adjacency.get(parent.getId()).add(child.getId());
        DagAlgorithms.assertAcyclic(taskIds(tasks), adjacency);

        PipelineDependency dependency = new PipelineDependency();
        dependency.setPipeline(parent.getPipeline());
        dependency.setParentTask(parent);
        dependency.setChildTask(child);
        return dependencyMapper.toResponse(dependencyRepository.save(dependency));
    }

    @Transactional
    public void deleteDependency(UUID pipelineId, UUID dependencyId) {
        PipelineDependency dependency = dependencyRepository.findById(dependencyId)
                .filter(d -> d.getPipeline().getId().equals(pipelineId))
                .orElseThrow(() -> new ResourceNotFoundException("Dependency not found: " + dependencyId));
        dependencyRepository.delete(dependency);
    }

    @Transactional(readOnly = true)
    public DagResponse getDag(UUID pipelineId) {
        requirePipeline(pipelineId);
        List<PipelineTask> tasks = taskRepository.findByPipelineId(pipelineId);
        List<PipelineDependency> edges = dependencyRepository.findByPipelineId(pipelineId);

        List<UUID> order = DagAlgorithms.topologicalSort(taskIds(tasks), buildAdjacency(tasks, edges));

        return new DagResponse(
                pipelineId,
                tasks.stream().map(taskMapper::toResponse).toList(),
                edges.stream().map(dependencyMapper::toResponse).toList(),
                order);
    }

    /** Validates the pipeline's DAG, throwing {@code PipelineCycleException} if cyclic. */
    @Transactional(readOnly = true)
    public void validate(UUID pipelineId) {
        requirePipeline(pipelineId);
        List<PipelineTask> tasks = taskRepository.findByPipelineId(pipelineId);
        List<PipelineDependency> edges = dependencyRepository.findByPipelineId(pipelineId);
        DagAlgorithms.assertAcyclic(taskIds(tasks), buildAdjacency(tasks, edges));
    }

    private Map<UUID, List<UUID>> buildAdjacency(List<PipelineTask> tasks, List<PipelineDependency> edges) {
        Map<UUID, List<UUID>> adjacency = new HashMap<>();
        for (PipelineTask task : tasks) {
            adjacency.put(task.getId(), new ArrayList<>());
        }
        for (PipelineDependency edge : edges) {
            adjacency.get(edge.getParentTask().getId()).add(edge.getChildTask().getId());
        }
        return adjacency;
    }

    private List<UUID> taskIds(List<PipelineTask> tasks) {
        return tasks.stream().map(PipelineTask::getId).toList();
    }

    private Pipeline requirePipeline(UUID pipelineId) {
        return pipelineRepository.findById(pipelineId)
                .orElseThrow(() -> new ResourceNotFoundException("Pipeline not found: " + pipelineId));
    }

    private PipelineTask requireTaskInPipeline(UUID pipelineId, UUID taskId) {
        return taskRepository.findById(taskId)
                .filter(t -> t.getPipeline().getId().equals(pipelineId))
                .orElseThrow(() -> new ResourceNotFoundException("Task not found in pipeline: " + taskId));
    }
}
