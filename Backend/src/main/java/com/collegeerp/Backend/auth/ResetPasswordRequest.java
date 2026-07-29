package com.collegeerp.Backend.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * {@code collegeCode} tells the backend which schema to look up {@code token} in
 * (each tenant schema - and the public schema, for the super admin - has its own
 * password_reset_tokens table), the same way it tells {@link LoginRequest} which
 * tenant to authenticate against. The reset link built by the forgot-password email
 * carries it as a query param so the reset-password page can send it back here
 * without the user having to type it again.
 */
public record ResetPasswordRequest(

        @NotBlank(message = "College code is required")
        String collegeCode,

        @NotBlank(message = "Reset token is required")
        String token,

        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String newPassword
) {}
