package com.collegeerp.Backend.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
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
        // Always use UTF-8 explicitly so the signing key is deterministic across
        // operating systems and JVM defaults.
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
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
     * Access token variant carrying fine-grained permissions so @PreAuthorize
     * checks do not require a database lookup on every request.
     */
    public String generateAccessToken(Long id, String username, String schemaName, String role,
                                      java.util.Collection<String> permissions) {
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
     * Issues a refresh token containing only the account identity needed to re-derive
     * a fresh access token. Role and permissions are intentionally re-read from the DB
     * during refresh so permission changes take effect without a full re-login.
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
        if (id == null) {
            throw new IllegalArgumentException("JWT is missing user id");
        }
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
        String schema = extractClaims(token).get("schema", String.class);
        if (schema == null || schema.isBlank()) {
            throw new IllegalArgumentException("JWT is missing tenant schema");
        }
        return schema;
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
