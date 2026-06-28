package com.pipeforge.execution.mapper;

import com.pipeforge.execution.dto.ExecutionResponse;
import com.pipeforge.execution.entity.PipelineRun;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ExecutionMapper {

    @Mapping(target = "pipelineId", source = "pipeline.id")
    ExecutionResponse toResponse(PipelineRun run);
}
