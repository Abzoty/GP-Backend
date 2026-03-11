package com.gp.GP_backend.shared.exception;

import com.gp.GP_backend.shared.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Centralised exception → HTTP response mapping.
 *
 * <p>
 * All {@code @ExceptionHandler} methods return an {@link ApiResponse} envelope
 * so every error response has the same structure as a success response. This
 * keeps
 * the client-side error handling consistent.
 *
 * <p>
 * Handler specificity: Spring picks the most specific handler. Add more
 * specific
 * exceptions above the {@link Exception} catch-all to give clients better error
 * messages.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ── Validation (400) ───────────────────────────────────────────────────────

    /**
     * Triggered when a {@code @Valid}-annotated request body fails Jakarta Bean
     * Validation.
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
     * Thrown by service-layer guard clauses (e.g. duplicate email, resource not
     * found).
     * Maps to 400 Bad Request — the client sent semantically invalid data.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArg(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
    }

    // ── Authentication (401) ───────────────────────────────────────────────────

    /**
     * Thrown by Spring Security when email or password is wrong.
     * Returns 401 with a generic message to avoid leaking whether the email exists.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail("Invalid email or password"));
    }

    /**
     * Thrown when a user attempts to log in but their account is soft-deleted
     * ({@link com.gp.GP_backend.domain.user.entity.User#isActive} = false).
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisabled(DisabledException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail("Account is disabled. Please contact support."));
    }

    /**
     * Thrown when a user account is locked (for future brute-force protection).
     */
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleLocked(LockedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail("Account is locked. Please contact support."));
    }

    // ── Catch-all (500) ────────────────────────────────────────────────────────

    /**
     * Safety net for any unhandled exception.
     * Logs the full stack trace for server-side debugging but returns a generic
     * message to the client to avoid leaking internal details.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("An unexpected error occurred. Please try again later."));
    }
}