package com.pipeforge.auth.service;

import com.pipeforge.auth.dto.AuthResponse;
import com.pipeforge.auth.dto.LoginRequest;
import com.pipeforge.auth.dto.RefreshRequest;
import com.pipeforge.auth.dto.SignupRequest;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

/**
 * Authentication use-cases (App Flow §4.2–4.3, Technical Requirements §3):
 * signup, login, token refresh (with rotation), and logout.
 */
@Service
public class AuthService {

    private static final Base64.Encoder TOKEN_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final UserMapper userMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       JwtProperties jwtProperties,
                       UserMapper userMapper) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.userMapper = userMapper;
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ValidationException("Email already registered");
        }
        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        // Role is assigned server-side; new accounts default to ENGINEER (PRD primary user).
        user.setRole(Role.ENGINEER);
        user.setActive(true);

        return issueTokens(userRepository.save(user));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        if (!user.isActive()) {
            throw new UnauthorizedException("Account is disabled");
        }
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token expired or revoked");
        }
        // Rotate: invalidate the presented token and issue a fresh pair.
        stored.setRevoked(true);
        return issueTokens(stored.getUser());
    }

    @Transactional
    public void logout(RefreshRequest request) {
        refreshTokenRepository.findByToken(request.refreshToken())
                .ifPresent(token -> refreshTokenRepository.revokeAllActiveForUser(token.getUser().getId()));
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(generateRefreshTokenValue());
        refreshToken.setExpiresAt(Instant.now().plusMillis(jwtProperties.getRefreshTokenExpiry()));
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.bearer(accessToken, refreshToken.getToken(),
                jwtService.getAccessTokenExpirySeconds(), userMapper.toResponse(user));
    }

    private String generateRefreshTokenValue() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return TOKEN_ENCODER.encodeToString(bytes);
    }
}
