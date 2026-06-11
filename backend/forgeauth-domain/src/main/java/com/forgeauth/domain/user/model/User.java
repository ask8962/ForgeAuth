package com.forgeauth.domain.user.model;

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
public class User {
    private UUID id;
    private String email;
    private boolean emailVerified;
    private String passwordHash;
    private String displayName;
    private String avatarUrl;
    
    @Builder.Default
    private AccountStatus status = AccountStatus.ACTIVE;
    
    @Builder.Default
    private int failedLoginAttempts = 0;
    
    private Instant lockedUntil;
    private Instant createdAt;
    private Instant updatedAt;

    public void incrementFailedAttempts() {
        this.failedLoginAttempts++;
    }

    public void lockAccount(Instant until) {
        this.status = AccountStatus.LOCKED;
        this.lockedUntil = until;
    }

    public void resetFailedAttempts() {
        this.failedLoginAttempts = 0;
        if (this.status == AccountStatus.LOCKED) {
            this.status = AccountStatus.ACTIVE;
            this.lockedUntil = null;
        }
    }
}
