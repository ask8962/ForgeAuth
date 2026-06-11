package com.forgeauth.common.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ErrorResponse {
    private ErrorDetails error;

    @Data
    @Builder
    public static class ErrorDetails {
        private String code;
        private String message;
        private int status;
        private Instant timestamp;
        private String traceId;
    }
}
