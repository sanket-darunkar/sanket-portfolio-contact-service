package dev.sanket.contactservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * Uniform error response envelope returned by the global exception handler.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        int status,
        String error,
        String message,
        Instant timestamp,
        Map<String, String> fieldErrors
) {

    /** Convenience factory for single-message errors. */
    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(status, error, message, Instant.now(), null);
    }

    /** Convenience factory for validation errors with per-field detail. */
    public static ErrorResponse validation(int status, String message, Map<String, String> fieldErrors) {
        return new ErrorResponse(status, "Validation Failed", message, Instant.now(), fieldErrors);
    }
}
