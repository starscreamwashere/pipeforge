package com.pipeforge.pipeline.mapper;

import com.pipeforge.pipeline.dto.DependencyResponse;
import com.pipeforge.pipeline.entity.PipelineDependency;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DependencyMapper {

    @Mapping(target = "pipelineId", source = "pipeline.id")
    @Mapping(target = "parentTaskId", source = "parentTask.id")
    @Mapping(target = "childTaskId", source = "childTask.id")
    DependencyResponse toResponse(PipelineDependency dependency);
}
