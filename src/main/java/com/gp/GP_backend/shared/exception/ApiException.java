package com.gp.GP_backend.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

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
