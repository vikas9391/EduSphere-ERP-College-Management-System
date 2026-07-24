package com.collegeerp.Backend.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Login payload for {@code POST /api/auth/super-admin/login}. Unlike {@link LoginRequest},
 * there is no {@code collegeCode} — a super admin isn't scoped to any tenant, so this
 * endpoint authenticates directly against the public-schema {@code super_admins} table.
 */
public record SuperAdminLoginRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {}
