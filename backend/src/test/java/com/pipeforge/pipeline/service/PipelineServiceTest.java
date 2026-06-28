package com.pipeforge.pipeline.service;

import com.pipeforge.auth.entity.User;
import com.pipeforge.auth.repository.UserRepository;
import com.pipeforge.exception.ResourceNotFoundException;
import com.pipeforge.pipeline.dto.CreatePipelineRequest;
import com.pipeforge.pipeline.dto.PipelineResponse;
import com.pipeforge.pipeline.dto.UpdatePipelineRequest;
import com.pipeforge.pipeline.entity.Pipeline;
import com.pipeforge.pipeline.entity.PipelineStatus;
import com.pipeforge.pipeline.entity.RetryPolicy;
import com.pipeforge.pipeline.mapper.PipelineMapper;
import com.pipeforge.pipeline.repository.PipelineRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PipelineServiceTest {

    @Mock PipelineRepository pipelineRepository;
    @Mock UserRepository userRepository;
    @Mock PipelineMapper pipelineMapper;
    @Mock com.pipeforge.scheduler.PipelineSchedulerService schedulerService;

    @InjectMocks PipelineService pipelineService;

    @Test
    void createDefaultsStatusVersionRetryAndOwner() {
        UUID ownerId = UUID.randomUUID();
        User ownerRef = new User();
        ownerRef.setId(ownerId);
        when(userRepository.getReferenceById(ownerId)).thenReturn(ownerRef);
        when(pipelineRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        pipelineService.create(new CreatePipelineRequest("etl", "desc", "0 0 * * * *", null), ownerId);

        ArgumentCaptor<Pipeline> captor = ArgumentCaptor.forClass(Pipeline.class);
        verify(pipelineRepository).save(captor.capture());
        Pipeline saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("etl");
        assertThat(saved.getOwner()).isSameAs(ownerRef);
        assertThat(saved.getStatus()).isEqualTo(PipelineStatus.DRAFT);
        assertThat(saved.getVersion()).isEqualTo(1);
        assertThat(saved.getRetryPolicy()).isEqualTo(RetryPolicy.defaults());
    }

    @Test
    void getReturnsMappedResponse() {
        UUID id = UUID.randomUUID();
        Pipeline pipeline = new Pipeline();
        PipelineResponse expected = mock();
        when(pipelineRepository.findById(id)).thenReturn(Optional.of(pipeline));
        when(pipelineMapper.toResponse(pipeline)).thenReturn(expected);

        assertThat(pipelineService.get(id)).isSameAs(expected);
    }

    @Test
    void getMissingThrowsNotFound() {
        UUID id = UUID.randomUUID();
        when(pipelineRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pipelineService.get(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateAppliesChangesAndSaves() {
        UUID id = UUID.randomUUID();
        Pipeline pipeline = new Pipeline();
        when(pipelineRepository.findById(id)).thenReturn(Optional.of(pipeline));
        when(pipelineRepository.save(pipeline)).thenReturn(pipeline);

        UpdatePipelineRequest request = new UpdatePipelineRequest("new", null, null, null, PipelineStatus.ACTIVE);
        pipelineService.update(id, request);

        verify(pipelineMapper).updateEntity(request, pipeline);
        verify(pipelineRepository).save(pipeline);
    }

    @Test
    void deleteSoftDeletesPipeline() {
        UUID id = UUID.randomUUID();
        Pipeline pipeline = new Pipeline();
        when(pipelineRepository.findById(id)).thenReturn(Optional.of(pipeline));

        pipelineService.delete(id);

        assertThat(pipeline.isDeleted()).isTrue();
        verify(pipelineRepository).save(pipeline);
    }

    private static PipelineResponse mock() {
        return new PipelineResponse(UUID.randomUUID(), "etl", null, null, null,
                UUID.randomUUID(), "Ada", PipelineStatus.DRAFT, 1, null, null);
    }
}
