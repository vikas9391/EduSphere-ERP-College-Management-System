package com.collegeerp.Backend.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Login payload. Validation annotations ensure malformed requests are rejected
 * by GlobalExceptionHandler with a 400 before ever reaching the database.
 *
 * <p>{@code collegeCode} is always required, including for the super admin: it just uses
 * a reserved code (see {@code AuthController.superAdminCode}) instead of a real tenant's
 * subdomain. This keeps one login form, one field, and one endpoint for every role,
 * without exposing a "log in as super admin" toggle in the UI.</p>
 */
public record LoginRequest(

        @NotBlank(message = "College code is required")
        String collegeCode,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {}
