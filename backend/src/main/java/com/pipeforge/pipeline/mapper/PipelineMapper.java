package com.pipeforge.pipeline.mapper;

import com.pipeforge.pipeline.dto.PipelineResponse;
import com.pipeforge.pipeline.dto.UpdatePipelineRequest;
import com.pipeforge.pipeline.entity.Pipeline;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/** Maps between {@link Pipeline} entities and their DTOs. */
@Mapper(componentModel = "spring")
public interface PipelineMapper {

    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "ownerName", source = "owner.name")
    PipelineResponse toResponse(Pipeline pipeline);

    /** Applies non-null fields of the request onto an existing pipeline (partial update). */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntity(UpdatePipelineRequest request, @MappingTarget Pipeline pipeline);
}
