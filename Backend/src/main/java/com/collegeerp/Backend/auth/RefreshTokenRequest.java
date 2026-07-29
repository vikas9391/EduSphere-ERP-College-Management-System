package com.collegeerp.Backend.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload for {@code POST /api/auth/refresh}. The refresh token itself carries enough
 * (account id, schema, account type) for the server to re-derive a fresh access token
 * without the caller re-sending credentials.
 */
public record RefreshTokenRequest(

        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {}
