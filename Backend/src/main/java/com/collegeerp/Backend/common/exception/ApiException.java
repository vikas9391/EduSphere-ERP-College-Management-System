package com.collegeerp.Backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base type for all application-specific exceptions.
 * Every subclass carries the {@link HttpStatus} that {@code GlobalExceptionHandler}
 * should translate it into, so controllers/services never need to know about HTTP concerns.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    protected ApiException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
