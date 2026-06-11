package com.forgeauth.infrastructure.persistence.adapter;

import com.forgeauth.domain.session.model.Session;
import com.forgeauth.domain.session.repository.SessionRepository;
import com.forgeauth.infrastructure.persistence.entity.SessionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class SessionRepositoryAdapter implements SessionRepository {

    private final JpaSessionRepository jpaSessionRepository;

    @Override
    public Optional<Session> findById(UUID id) {
        return jpaSessionRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Session> findByTokenHash(String tokenHash) {
        return jpaSessionRepository.findByTokenHash(tokenHash).map(this::toDomain);
    }

    @Override
    public List<Session> findByUserId(UUID userId) {
        return jpaSessionRepository.findByUserId(userId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Session save(Session session) {
        SessionEntity entity = toEntity(session);
        SessionEntity saved = jpaSessionRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public void revokeAllForUser(UUID userId) {
        jpaSessionRepository.revokeAllForUser(userId, java.time.Instant.now());
    }

    private Session toDomain(SessionEntity entity) {
        return Session.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .tokenHash(entity.getTokenHash())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .deviceType(entity.getDeviceType())
                .browser(entity.getBrowser())
                .os(entity.getOs())
                .location(entity.getLocation())
                .createdAt(entity.getCreatedAt())
                .expiresAt(entity.getExpiresAt())
                .lastActiveAt(entity.getLastActiveAt())
                .revoked(entity.isRevoked())
                .revokedAt(entity.getRevokedAt())
                .build();
    }

    private SessionEntity toEntity(Session domain) {
        SessionEntity entity = new SessionEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setTokenHash(domain.getTokenHash());
        entity.setIpAddress(domain.getIpAddress());
        entity.setUserAgent(domain.getUserAgent());
        entity.setDeviceType(domain.getDeviceType());
        entity.setBrowser(domain.getBrowser());
        entity.setOs(domain.getOs());
        entity.setLocation(domain.getLocation());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setExpiresAt(domain.getExpiresAt());
        entity.setLastActiveAt(domain.getLastActiveAt());
        entity.setRevoked(domain.isRevoked());
        entity.setRevokedAt(domain.getRevokedAt());
        return entity;
    }
}
