package com.gp.GP_backend.shared.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Generic HTTP response envelope used by every endpoint in the API.
 *
 * <p>
 * Having a consistent envelope means the frontend can always expect:
 * 
 * <pre>
 * {
 *   "success"  : true | false,
 *   "message"  : "Human-readable status",
 *   "data"     : { ... } | null,
 *   "timestamp": "2025-01-01T00:00:00Z"
 * }
 * </pre>
 *
 * <p>
 * Use the static factory methods {@link #ok} and {@link #fail} rather than the
 * builder directly — they enforce the correct {@code success} flag.
 *
 * @param <T> the type of the {@code data} payload (use {@code Void} for empty
 *            responses)
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {

    /** {@code true} on success (2xx), {@code false} on error (4xx / 5xx). */
    private boolean success;

    /** Short human-readable description of the result. */
    private String message;

    /**
     * The response payload, or {@code null} for operations that produce no data.
     */
    private T data;

    /**
     * Server-side UTC timestamp of the response — useful for debugging clock-skew.
     */
    @Builder.Default
    private Instant timestamp = Instant.now();

    // ── Factory methods ────────────────────────────────────────────────────────

    /** Creates a success response with a message and data payload. */
    public static <T> ApiResponse<T> ok(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    /** Creates a failure response with an error message and no data. */
    public static <T> ApiResponse<T> fail(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
}