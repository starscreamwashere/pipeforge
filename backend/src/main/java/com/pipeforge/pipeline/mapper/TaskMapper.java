package com.pipeforge.pipeline.mapper;

import com.pipeforge.pipeline.dto.TaskResponse;
import com.pipeforge.pipeline.entity.PipelineTask;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    @Mapping(target = "pipelineId", source = "pipeline.id")
    TaskResponse toResponse(PipelineTask task);
}
