package com.collegeerp.Backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an authenticated user is not allowed to perform an action on a specific
 * resource - wrong role (e.g. a student calling a teacher-only endpoint) or wrong
 * ownership (e.g. a teacher trying to modify another teacher's class). Distinct from
 * {@link InvalidCredentialsException} (401 - who are you?) and Spring Security's
 * {@code AccessDeniedException} (used by the filter chain itself); this one is for
 * checks made explicitly in service/controller code. Maps to HTTP 403.
 */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
