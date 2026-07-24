package com.collegeerp.Backend.auth.dto;

/**
 * Response body for a successful login. Replaces the previous endpoint behaviour of
 * returning the JWT as a bare string, which gave API consumers no way to know the
 * token type, the authenticated user's role, or which tenant they landed in.
 */
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInMillis,
        String email,
        String role,
        String tenantSchema
) {
    public static LoginResponse of(String accessToken, long expiresInMillis, String email, String role, String tenantSchema) {
        return new LoginResponse(accessToken, "Bearer", expiresInMillis, email, role, tenantSchema);
    }
}
