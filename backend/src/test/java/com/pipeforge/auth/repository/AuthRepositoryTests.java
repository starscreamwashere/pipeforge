package com.pipeforge.auth.repository;

import com.pipeforge.TestcontainersConfiguration;
import com.pipeforge.auth.entity.RefreshToken;
import com.pipeforge.auth.entity.Role;
import com.pipeforge.auth.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository behaviour for the auth domain (Phase 2.3 — "repository tests pass").
 * Reuses the shared Testcontainers context; each test rolls back.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class AuthRepositoryTests {

    @Autowired
    UserRepository userRepository;

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    private User persistedUser(String email) {
        User user = new User();
        user.setName("Ada Lovelace");
        user.setEmail(email);
        user.setPasswordHash("$2a$10$hash");
        user.setRole(Role.ENGINEER);
        return userRepository.save(user);
    }

    @Test
    void persistsUserAndAssignsAuditFields() {
        User saved = persistedUser("ada@pipeforge.dev");

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    void findByEmailAndExistsByEmail() {
        persistedUser("grace@pipeforge.dev");

        assertThat(userRepository.findByEmail("grace@pipeforge.dev")).isPresent();
        assertThat(userRepository.existsByEmail("grace@pipeforge.dev")).isTrue();
        assertThat(userRepository.existsByEmail("nobody@pipeforge.dev")).isFalse();
    }

    @Test
    void findsRefreshTokenByValueAndRevokesActiveOnes() {
        User user = persistedUser("alan@pipeforge.dev");

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setToken("refresh-token-value");
        token.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        refreshTokenRepository.save(token);

        assertThat(refreshTokenRepository.findByToken("refresh-token-value")).isPresent();

        int revoked = refreshTokenRepository.revokeAllActiveForUser(user.getId());
        assertThat(revoked).isEqualTo(1);
    }
}
