package com.forgeauth.domain.session.repository;

import com.forgeauth.domain.session.model.Session;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepository {
    Optional<Session> findById(UUID id);
    Optional<Session> findByTokenHash(String tokenHash);
    List<Session> findByUserId(UUID userId);
    Session save(Session session);
    void revokeAllForUser(UUID userId);
}
