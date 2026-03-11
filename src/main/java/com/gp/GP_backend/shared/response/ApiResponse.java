package com.gp.GP_backend.shared.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Standard API envelope returned by every endpoint.
 *
 * <p>Every response, success or failure, uses this wrapper so that clients
 * always get a consistent shape to parse:
 * <pre>{@code
 * {
 * "success": true,
 * "message": "Login successful",
 * "timestamp": "2025-01-01T12:00:00Z",
 * "data": { ... }
 * }
 * }</pre>
 *
 * @param <T> the type of the {@code data} payload (use {@code Void} when there
 * is no body).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    /** Server-side timestamp of when the response was generated (UTC). */
    @Builder.Default
    private Instant timestamp = Instant.now();

    /** Convenience factory for successful responses. */
    public static <T> ApiResponse<T> ok(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    /** Convenience factory for error responses (data will be null). */
    public static <T> ApiResponse<T> fail(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
}
