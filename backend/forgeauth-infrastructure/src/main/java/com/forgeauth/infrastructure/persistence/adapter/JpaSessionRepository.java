package com.forgeauth.infrastructure.persistence.adapter;

import com.forgeauth.infrastructure.persistence.entity.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaSessionRepository extends JpaRepository<SessionEntity, UUID> {
    Optional<SessionEntity> findByTokenHash(String tokenHash);
    List<SessionEntity> findByUserId(UUID userId);

    @Modifying
    @Query("UPDATE SessionEntity s SET s.revoked = true, s.revokedAt = :now WHERE s.userId = :userId AND s.revoked = false")
    void revokeAllForUser(UUID userId, java.time.Instant now);
}
