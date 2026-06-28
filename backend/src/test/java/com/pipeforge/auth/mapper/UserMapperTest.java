package com.pipeforge.auth.mapper;

import com.pipeforge.auth.dto.UserResponse;
import com.pipeforge.auth.entity.Role;
import com.pipeforge.auth.entity.User;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MapStruct mapping for the auth domain (Phase 2.4 — "mapper tests pass").
 * Uses the generated implementation directly (no Spring context needed).
 */
class UserMapperTest {

    private final UserMapper mapper = new UserMapperImpl();

    @Test
    void mapsUserToResponseWithoutLeakingPasswordHash() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName("Ada Lovelace");
        user.setEmail("ada@pipeforge.dev");
        user.setPasswordHash("$2a$10$secret-hash");
        user.setRole(Role.ENGINEER);
        user.setActive(true);

        UserResponse response = mapper.toResponse(user);

        assertThat(response.id()).isEqualTo(user.getId());
        assertThat(response.name()).isEqualTo("Ada Lovelace");
        assertThat(response.email()).isEqualTo("ada@pipeforge.dev");
        assertThat(response.role()).isEqualTo(Role.ENGINEER);
        assertThat(response.active()).isTrue();
        // UserResponse has no field that could carry the password hash.
        assertThat(UserResponse.class.getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName)
                .doesNotContain("passwordHash", "password");
    }
}
