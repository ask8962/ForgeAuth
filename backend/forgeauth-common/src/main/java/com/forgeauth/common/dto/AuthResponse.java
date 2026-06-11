package com.forgeauth.common.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private UserDto user;
    
    // For MFA flows
    private boolean mfaRequired;
    private String mfaToken;

    @Data
    @Builder
    public static class UserDto {
        private String id;
        private String email;
        private String displayName;
        private boolean mfaEnabled;
    }
}
