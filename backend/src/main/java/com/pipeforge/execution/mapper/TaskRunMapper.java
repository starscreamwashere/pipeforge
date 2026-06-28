package com.pipeforge.execution.mapper;

import com.pipeforge.execution.dto.TaskRunResponse;
import com.pipeforge.execution.entity.TaskRun;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TaskRunMapper {

    @Mapping(target = "pipelineRunId", source = "pipelineRun.id")
    @Mapping(target = "taskId", source = "task.id")
    @Mapping(target = "taskName", source = "task.taskName")
    TaskRunResponse toResponse(TaskRun taskRun);
}
