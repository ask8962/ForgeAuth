package com.forgeauth.server.service;

import com.forgeauth.common.dto.AuthResponse;
import com.forgeauth.common.dto.LoginRequest;
import com.forgeauth.common.dto.RegisterRequest;
import com.forgeauth.common.dto.TokenRefreshRequest;
import com.forgeauth.common.exception.ApiException;
import com.forgeauth.domain.session.model.Session;
import com.forgeauth.domain.session.repository.SessionRepository;
import com.forgeauth.domain.user.model.AccountStatus;
import com.forgeauth.domain.user.model.User;
import com.forgeauth.domain.user.repository.UserRepository;
import com.forgeauth.infrastructure.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthApplicationService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${forgeauth.jwt.refresh-token-expiration-sec:604800}")
    private long refreshTokenExpirationSec;

    @Transactional
    public AuthResponse register(RegisterRequest request, String ipAddress, String userAgent) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw ApiException.badRequest("Email already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .displayName(request.getDisplayName() != null ? request.getDisplayName() : request.getEmail().split("@")[0])
                .status(AccountStatus.ACTIVE)
                .emailVerified(false)
                .failedLoginAttempts(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        user = userRepository.save(user);
        return createAuthResponse(user, ipAddress, userAgent);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> ApiException.unauthorized("Invalid credentials"));

        // Check lock status
        if (user.getStatus() == AccountStatus.LOCKED || user.getLockedUntil() != null) {
            if (user.getLockedUntil() != null && user.getLockedUntil().isBefore(Instant.now())) {
                // Lock expired, reset failed attempts and status
                user.resetFailedAttempts();
                userRepository.save(user);
            } else {
                throw ApiException.unauthorized("Account is locked. Please try again later.");
            }
        }

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw ApiException.unauthorized("Account is not active");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            user.incrementFailedAttempts();
            if (user.getFailedLoginAttempts() >= 5) {
                user.lockAccount(Instant.now().plusSeconds(900)); // Lock for 15 minutes
                userRepository.save(user);
                throw ApiException.unauthorized("Account has been locked due to too many failed attempts.");
            }
            userRepository.save(user);
            throw ApiException.unauthorized("Invalid credentials");
        }

        // Reset failed attempts on successful login
        if (user.getFailedLoginAttempts() > 0) {
            user.resetFailedAttempts();
            userRepository.save(user);
        }

        return createAuthResponse(user, ipAddress, userAgent);
    }

    private AuthResponse createAuthResponse(User user, String ipAddress, String userAgent) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        Session session = Session.builder()
                .userId(user.getId())
                .tokenHash(com.forgeauth.infrastructure.security.TokenHashUtil.sha256(refreshToken))
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .expiresAt(Instant.now().plusSeconds(refreshTokenExpirationSec))
                .lastActiveAt(Instant.now())
                .revoked(false)
                .createdAt(Instant.now())
                .build();

        sessionRepository.save(session);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationSec())
                .user(AuthResponse.UserDto.builder()
                        .id(user.getId().toString())
                        .email(user.getEmail())
                        .displayName(user.getDisplayName())
                        .mfaEnabled(false)
                        .build())
                .build();
    }

    @Transactional
    public void logout(String refreshToken) {
        sessionRepository.findByTokenHash(com.forgeauth.infrastructure.security.TokenHashUtil.sha256(refreshToken))
                .ifPresent(session -> {
                    session.revoke();
                    sessionRepository.save(session);
                    log.info("Session revoked for user: {}", session.getUserId());
                });
    }

    @Transactional
    public void logout(String refreshToken, UUID userId) {
        sessionRepository.findByTokenHash(com.forgeauth.infrastructure.security.TokenHashUtil.sha256(refreshToken))
                .ifPresent(session -> {
                    if (session.getUserId().equals(userId)) {
                        session.revoke();
                        sessionRepository.save(session);
                        log.info("Session revoked for user: {}", userId);
                    }
                });
    }

    @Transactional
    public AuthResponse refresh(TokenRefreshRequest request, String ipAddress, String userAgent) {
        String tokenHash = com.forgeauth.infrastructure.security.TokenHashUtil.sha256(request.getRefreshToken());
        
        Session session = sessionRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> ApiException.unauthorized("Invalid refresh token"));
                
        if (session.isRevoked() || session.getExpiresAt().isBefore(Instant.now())) {
            throw ApiException.unauthorized("Refresh token expired or revoked");
        }
        
        User user = userRepository.findById(session.getUserId())
                .orElseThrow(() -> ApiException.unauthorized("User not found"));
                
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw ApiException.unauthorized("Account is not active");
        }
        
        // Refresh token rotation: revoke old session, create new
        session.revoke();
        sessionRepository.save(session);
        
        return createAuthResponse(user, ipAddress, userAgent);
    }
}
