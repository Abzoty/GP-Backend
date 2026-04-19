package com.gp.GP_backend.shared.exception;

import com.gp.GP_backend.shared.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


import java.util.stream.Collectors;

/**
 * Centralized exception handler for all REST controllers.
 *
 * <p>
 * Spring's {@code @RestControllerAdvice} intercepts exceptions thrown from
 * any {@code @RestController} and converts them to structured
 * {@link ApiResponse} JSON, so controllers never need their own try/catch
 * blocks.
 *
 * <p>
 * Handler priority: more specific exceptions are listed first.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles Bean Validation failures ({@code @Valid} annotation).
     * Collects all field errors into a single comma-separated message.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(ApiResponse.fail(errors));
    }

    /**
     * Handles our custom {@link ApiException} — the primary way services
     * signal business rule violations (409 CONFLICT, 404 NOT FOUND, etc.).
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponse.fail(ex.getMessage()));
    }

    /**
     * Handles Spring Security's bad-credentials exception (wrong email/password).
     * Returns 401 with a deliberately vague message to avoid user enumeration.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail("Invalid email or password"));
    }

    /**
     * Handles access-denied exceptions from Spring Security's method-level
     * authorisation ({@code @PreAuthorize}, etc.).
     */
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AuthorizationDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.fail("You do not have permission to perform this action"));
    }


    /**
     * Handles Spring's {@link org.springframework.web.server.ResponseStatusException}
     * — preserves the status code and reason phrase the caller intended.
     */
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(
            org.springframework.web.server.ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(ApiResponse.fail(ex.getReason()));
    }


    /**
     * Catch-all handler for any unhandled exception.
     * Logs the full stack trace (for server-side debugging) but returns a
     * generic message to the client (to avoid leaking implementation details).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("An unexpected error occurred. Please try again later."));
    }

    /**
     * Handles malformed JSON in request bodies.
     * The exception message often contains the specific syntax error, which can
     * be helpful for clients.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail("Malformed JSON request: " + ex.getMostSpecificCause().getMessage()));
    }
}
