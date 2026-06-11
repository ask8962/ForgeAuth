package com.forgeauth.domain.session.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Session {
    private UUID id;
    private UUID userId;
    private String tokenHash;
    private String ipAddress;
    private String userAgent;
    private String deviceType;
    private String browser;
    private String os;
    private String location;
    
    private Instant createdAt;
    private Instant expiresAt;
    private Instant lastActiveAt;
    
    @Builder.Default
    private boolean revoked = false;
    private Instant revokedAt;

    public boolean isValid() {
        return !revoked && expiresAt.isAfter(Instant.now());
    }

    public void revoke() {
        this.revoked = true;
        this.revokedAt = Instant.now();
    }
}
