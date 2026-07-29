package com.collegeerp.Backend.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Mirrors {@link LoginRequest}'s use of {@code collegeCode}: always required, and
 * the reserved super-admin code routes to the public-schema super admin flow
 * instead of resolving a tenant - see {@code AuthController#forgotPassword}.
 */
public record ForgotPasswordRequest(

        @NotBlank(message = "College code is required")
        String collegeCode,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email
) {}
