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

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateAccessToken(Long id, String username, String schemaName, String role) {
        return Jwts.builder()
                .subject(username)
                .claims(Map.of(
                        "id", id,
                        "schema", schemaName,
                        "role", role
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
                        "permissions", permissions
                ))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(getSigningKey())
                .compact();
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
