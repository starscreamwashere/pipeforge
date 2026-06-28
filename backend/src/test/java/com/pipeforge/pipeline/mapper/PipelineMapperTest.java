package com.pipeforge.pipeline.mapper;

import com.pipeforge.auth.entity.User;
import com.pipeforge.pipeline.dto.PipelineResponse;
import com.pipeforge.pipeline.dto.UpdatePipelineRequest;
import com.pipeforge.pipeline.entity.Pipeline;
import com.pipeforge.pipeline.entity.PipelineStatus;
import com.pipeforge.pipeline.entity.RetryPolicy;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PipelineMapperTest {

    private final PipelineMapper mapper = new PipelineMapperImpl();

    private Pipeline pipeline() {
        User owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setName("Ada");

        Pipeline p = new Pipeline();
        p.setId(UUID.randomUUID());
        p.setName("daily-sync");
        p.setStatus(PipelineStatus.ACTIVE);
        p.setRetryPolicy(new RetryPolicy(5, 10));
        p.setOwner(owner);
        p.setVersion(1);
        return p;
    }

    @Test
    void mapsEntityToResponseFlatteningOwner() {
        PipelineResponse response = mapper.toResponse(pipeline());

        assertThat(response.name()).isEqualTo("daily-sync");
        assertThat(response.ownerName()).isEqualTo("Ada");
        assertThat(response.ownerId()).isNotNull();
        assertThat(response.status()).isEqualTo(PipelineStatus.ACTIVE);
        assertThat(response.retryPolicy()).isEqualTo(new RetryPolicy(5, 10));
    }

    @Test
    void partialUpdateIgnoresNullFields() {
        Pipeline p = pipeline();
        // Only the name is provided; everything else stays as-is.
        mapper.updateEntity(new UpdatePipelineRequest("renamed", null, null, null, null), p);

        assertThat(p.getName()).isEqualTo("renamed");
        assertThat(p.getStatus()).isEqualTo(PipelineStatus.ACTIVE);
        assertThat(p.getRetryPolicy()).isEqualTo(new RetryPolicy(5, 10));
    }
}
