package com.forgeauth.common.exception;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {
    
    private final String code;
    private final int status;

    public ApiException(String code, String message, int status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public static ApiException unauthorized(String message) {
        return new ApiException("AUTH_INVALID_CREDENTIALS", message, 401);
    }

    public static ApiException notFound(String message) {
        return new ApiException("RESOURCE_NOT_FOUND", message, 404);
    }

    public static ApiException badRequest(String message) {
        return new ApiException("VALIDATION_ERROR", message, 400);
    }
}
