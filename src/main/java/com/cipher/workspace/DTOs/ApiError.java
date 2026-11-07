package com.cipher.workspace.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@Schema(description = "Standard API error response payload")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        @Schema(description = "Timestamp when error occurred", example = "2026-11-06T20:00:00Z")
        Instant timestamp,

        @Schema(description = "HTTP status code", example = "400")
        int status,

        @Schema(description = "HTTP error reason phrase", example = "Bad Request")
        String error,

        @Schema(description = "Descriptive error message", example = "Validation failed")
        String message,

        @Schema(description = "Request URI path where error occurred", example = "/api/auth/signin")
        String path,

        @Schema(description = "Field-level validation error details (if applicable)")
        Map<String, String> fieldErrors
) {
    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path, null);
    }

    public static ApiError of(int status, String error, String message, String path, Map<String, String> fieldErrors) {
        return new ApiError(Instant.now(), status, error, message, path, fieldErrors);
    }
}
