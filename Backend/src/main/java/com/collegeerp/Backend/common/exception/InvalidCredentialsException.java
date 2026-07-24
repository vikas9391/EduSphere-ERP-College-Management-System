package com.collegeerp.Backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when login credentials (email/password/college code) do not match any account.
 * Deliberately generic in message so callers cannot enumerate valid emails.
 * Maps to HTTP 401.
 */
public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
