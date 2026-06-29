package com.pipeforge.worker.mapper;

import com.pipeforge.worker.dto.WorkerResponse;
import com.pipeforge.worker.entity.Worker;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WorkerMapper {

    WorkerResponse toResponse(Worker worker);
}
