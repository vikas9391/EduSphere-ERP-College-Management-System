package com.collegeerp.Backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when provisioning a new tenant's database schema fails (schema creation,
 * Flyway migration, or seeding the initial admin user). Distinct from
 * {@link BadRequestException} because this is a server-side/infrastructure failure,
 * not a client input problem. Maps to HTTP 500.
 */
public class TenantProvisioningException extends ApiException {

    public TenantProvisioningException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
