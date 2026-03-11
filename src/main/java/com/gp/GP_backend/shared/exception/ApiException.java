package com.gp.GP_backend.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Application-level exception that carries an HTTP status code.
 *
 * <p>
 * Throw this instead of generic {@link RuntimeException} whenever you want
 * precise control over the HTTP response status. It is caught by
 * {@link GlobalExceptionHandler} and serialised into a standard
 * {@link com.gp.GP_backend.shared.response.ApiResponse}.
 *
 * <p>
 * Example:
 * 
 * <pre>{@code
 * throw new ApiException(HttpStatus.CONFLICT, "Email is already registered");
 * }</pre>
 */
@Getter
public class ApiException extends RuntimeException {

    /** The HTTP status code to return to the client. */
    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    /**
     * Wraps a cause exception while preserving the original stack trace.
     * Useful when rethrowing lower-level exceptions with a friendlier message.
     */
    public ApiException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }
}
