package com.pipeforge.pipeline.repository;

import com.pipeforge.TestcontainersConfiguration;
import com.pipeforge.auth.entity.Role;
import com.pipeforge.auth.entity.User;
import com.pipeforge.auth.repository.UserRepository;
import com.pipeforge.pipeline.entity.Pipeline;
import com.pipeforge.pipeline.entity.PipelineStatus;
import com.pipeforge.pipeline.entity.RetryPolicy;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class PipelineRepositoryTests {

    @Autowired UserRepository userRepository;
    @Autowired PipelineRepository pipelineRepository;
    @Autowired EntityManager entityManager;

    private User owner() {
        User u = new User();
        u.setName("Owner");
        u.setEmail("owner-" + System.nanoTime() + "@pipeforge.dev");
        u.setPasswordHash("hash");
        u.setRole(Role.ENGINEER);
        return userRepository.save(u);
    }

    private Pipeline pipeline(User owner, String name) {
        Pipeline p = new Pipeline();
        p.setName(name);
        p.setOwner(owner);
        p.setStatus(PipelineStatus.DRAFT);
        p.setRetryPolicy(RetryPolicy.defaults());
        return pipelineRepository.save(p);
    }

    @Test
    void persistsPipelineWithJsonRetryPolicyAndAuditFields() {
        Pipeline saved = pipeline(owner(), "daily-sync");

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getVersion()).isEqualTo(1);
        assertThat(saved.getRetryPolicy()).isEqualTo(RetryPolicy.defaults());
    }

    @Test
    void findsByOwnerId() {
        User owner = owner();
        pipeline(owner, "p1");
        pipeline(owner, "p2");

        var page = pipelineRepository.findByOwnerId(owner.getId(), PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void softDeletedPipelineIsHiddenFromQueries() {
        Pipeline p = pipeline(owner(), "to-delete");
        p.setDeleted(true);
        pipelineRepository.save(p);
        entityManager.flush();
        entityManager.clear();

        assertThat(pipelineRepository.findById(p.getId())).isEmpty();
    }
}
