package com.collegeerp.Backend.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private static final String TOKEN_TYPE_CLAIM = "typ";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";
    private static final String ACCOUNT_TYPE_CLAIM = "acct";

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateAccessToken(Long id, String username, String schemaName, String role) {
        return Jwts.builder()
                .subject(username)
                .claims(Map.of(
                        "id", id,
                        "schema", schemaName,
                        "role", role,
                        TOKEN_TYPE_CLAIM, TOKEN_TYPE_ACCESS
                ))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Same as {@link #generateAccessToken(Long, String, String, String)} but also embeds
     * the caller's fine-grained permissions (see {@link com.collegeerp.Backend.common.Permission})
     * as a claim, so {@code @PreAuthorize("hasAuthority(...)")} checks don't need a DB
     * round-trip on every request. Used for staff/admin logins (the only accounts that
     * go through the Role/Permission system) - teachers, students, and the super admin
     * keep using the 4-arg overload above.
     */
    public String generateAccessToken(Long id, String username, String schemaName, String role, java.util.Collection<String> permissions) {
        return Jwts.builder()
                .subject(username)
                .claims(Map.of(
                        "id", id,
                        "schema", schemaName,
                        "role", role,
                        "permissions", permissions,
                        TOKEN_TYPE_CLAIM, TOKEN_TYPE_ACCESS
                ))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Issues a long-lived refresh token, deliberately carrying only what's needed to
     * re-derive a fresh access token later: the account id, its schema, and which
     * repository it lives in ({@code accountType}: STAFF/TEACHER/STUDENT/SUPER_ADMIN).
     * No role or permissions are embedded here on purpose - {@code /api/auth/refresh}
     * re-reads those fresh from the DB every time, so a role edit or permission change
     * takes effect on the next silent refresh instead of being stuck until the user
     * fully re-logs-in.
     */
    public String generateRefreshToken(Long id, String username, String schemaName, String accountType) {
        return Jwts.builder()
                .subject(username)
                .claims(Map.of(
                        "id", id,
                        "schema", schemaName,
                        ACCOUNT_TYPE_CLAIM, accountType,
                        TOKEN_TYPE_CLAIM, TOKEN_TYPE_REFRESH
                ))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    /** True only for a token minted by {@link #generateRefreshToken}. Used both to stop
     *  a refresh token being used directly as an access token, and to stop an access
     *  token being handed to {@code /api/auth/refresh}. */
    public boolean isRefreshToken(String token) {
        return TOKEN_TYPE_REFRESH.equals(extractClaims(token).get(TOKEN_TYPE_CLAIM, String.class));
    }

    public boolean isAccessToken(String token) {
        return TOKEN_TYPE_ACCESS.equals(extractClaims(token).get(TOKEN_TYPE_CLAIM, String.class));
    }

    public String extractAccountType(String token) {
        return extractClaims(token).get(ACCOUNT_TYPE_CLAIM, String.class);
    }

    @SuppressWarnings("unchecked")
    public java.util.List<String> extractPermissions(String token) {
        java.util.List<String> permissions = extractClaims(token).get("permissions", java.util.List.class);
        return permissions == null ? java.util.List.of() : permissions;
    }

    public Long extractUserId(String token) {
        Number id = extractClaims(token).get("id", Number.class);
        return id.longValue();
    }
    public io.jsonwebtoken.Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractSchema(String token) {
        return extractClaims(token).get("schema", String.class);
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }
    public boolean isTokenValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (Exception e) {
            log.debug("Rejected invalid JWT: {}", e.getMessage());
            return false;
        }
    }
}
