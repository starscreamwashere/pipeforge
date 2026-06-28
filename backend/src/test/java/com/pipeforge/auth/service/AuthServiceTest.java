package com.pipeforge.auth.service;

import com.pipeforge.auth.dto.AuthResponse;
import com.pipeforge.auth.dto.LoginRequest;
import com.pipeforge.auth.dto.RefreshRequest;
import com.pipeforge.auth.dto.SignupRequest;
import com.pipeforge.auth.dto.UserResponse;
import com.pipeforge.auth.entity.RefreshToken;
import com.pipeforge.auth.entity.Role;
import com.pipeforge.auth.entity.User;
import com.pipeforge.auth.mapper.UserMapper;
import com.pipeforge.auth.repository.RefreshTokenRepository;
import com.pipeforge.auth.repository.UserRepository;
import com.pipeforge.auth.security.JwtService;
import com.pipeforge.config.JwtProperties;
import com.pipeforge.exception.UnauthorizedException;
import com.pipeforge.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock JwtProperties jwtProperties;
    @Mock UserMapper userMapper;

    @InjectMocks AuthService authService;

    private User user(String email) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setName("Ada");
        u.setEmail(email);
        u.setPasswordHash("hashed");
        u.setRole(Role.ENGINEER);
        u.setActive(true);
        return u;
    }

    private void stubTokenIssuance() {
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(jwtService.getAccessTokenExpirySeconds()).thenReturn(900L);
        when(jwtProperties.getRefreshTokenExpiry()).thenReturn(604_800_000L);
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userMapper.toResponse(any())).thenReturn(
                new UserResponse(UUID.randomUUID(), "Ada", "ada@pipeforge.dev", Role.ENGINEER, true));
    }

    @Test
    void signupCreatesUserAndIssuesTokens() {
        when(userRepository.existsByEmail("ada@pipeforge.dev")).thenReturn(false);
        when(passwordEncoder.encode("password1")).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        stubTokenIssuance();

        AuthResponse response = authService.signup(new SignupRequest("Ada", "ada@pipeforge.dev", "password1"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900L);
        verify(refreshTokenRepository).save(any());
    }

    @Test
    void signupRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("dup@pipeforge.dev")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(new SignupRequest("Dup", "dup@pipeforge.dev", "password1")))
                .isInstanceOf(ValidationException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginSucceedsWithValidCredentials() {
        User u = user("ada@pipeforge.dev");
        when(userRepository.findByEmail("ada@pipeforge.dev")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("password1", "hashed")).thenReturn(true);
        stubTokenIssuance();

        AuthResponse response = authService.login(new LoginRequest("ada@pipeforge.dev", "password1"));

        assertThat(response.accessToken()).isEqualTo("access-token");
    }

    @Test
    void loginRejectsUnknownEmail() {
        when(userRepository.findByEmail("ghost@pipeforge.dev")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost@pipeforge.dev", "password1")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void loginRejectsWrongPassword() {
        User u = user("ada@pipeforge.dev");
        when(userRepository.findByEmail("ada@pipeforge.dev")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("ada@pipeforge.dev", "wrong")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void refreshRotatesTokenAndIssuesNewPair() {
        User u = user("ada@pipeforge.dev");
        RefreshToken stored = new RefreshToken();
        stored.setUser(u);
        stored.setToken("old-refresh");
        stored.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));
        when(refreshTokenRepository.findByToken("old-refresh")).thenReturn(Optional.of(stored));
        stubTokenIssuance();

        AuthResponse response = authService.refresh(new RefreshRequest("old-refresh"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(stored.isRevoked()).isTrue();
    }

    @Test
    void refreshRejectsRevokedToken() {
        RefreshToken stored = new RefreshToken();
        stored.setUser(user("ada@pipeforge.dev"));
        stored.setToken("revoked");
        stored.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));
        stored.setRevoked(true);
        when(refreshTokenRepository.findByToken("revoked")).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("revoked")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void logoutRevokesAllActiveTokensForUser() {
        User u = user("ada@pipeforge.dev");
        RefreshToken stored = new RefreshToken();
        stored.setUser(u);
        stored.setToken("to-logout");
        when(refreshTokenRepository.findByToken("to-logout")).thenReturn(Optional.of(stored));

        authService.logout(new RefreshRequest("to-logout"));

        verify(refreshTokenRepository).revokeAllActiveForUser(u.getId());
    }
}
