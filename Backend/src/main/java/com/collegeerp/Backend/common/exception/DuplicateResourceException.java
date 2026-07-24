package com.collegeerp.Backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an operation would violate a uniqueness constraint
 * (e.g. registering a tenant subdomain or email that already exists).
 * Maps to HTTP 409.
 */
public class DuplicateResourceException extends ApiException {

    public DuplicateResourceException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
