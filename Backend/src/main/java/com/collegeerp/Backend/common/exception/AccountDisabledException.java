package com.collegeerp.Backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an authenticating account exists but is disabled/inactive.
 * Maps to HTTP 403.
 */
public class AccountDisabledException extends ApiException {

    public AccountDisabledException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
