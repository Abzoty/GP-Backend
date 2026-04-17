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

    private final HttpStatus status;
    private final String details;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
        this.details = null;
    }

    public ApiException(HttpStatus status, String message, String details) {
        super(message);
        this.status = status;
        this.details = details;
    }

    public ApiException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.details = null;
    }

    public String getDetails() {
        return details;
    }
}
