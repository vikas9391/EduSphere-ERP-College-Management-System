package com.collegeerp.Backend.security;

import com.collegeerp.Backend.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Extracts and validates an access JWT on every request, populates Spring Security
 * with the authenticated principal, and sets the tenant schema for the request.
 * TenantContext is always cleared in finally to prevent thread-pool leakage.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader(AUTH_HEADER);

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length()).trim();

            // Require an explicitly minted access token. Previously any valid JWT that
            // was not marked as a refresh token could reach protected endpoints.
            if (jwtService.isTokenValid(token) && jwtService.isAccessToken(token)) {
                String schema = jwtService.extractSchema(token);
                String username = jwtService.extractUsername(token);
                String role = jwtService.extractClaims(token).get("role", String.class);
                Long userId = jwtService.extractUserId(token);
                List<String> permissions = jwtService.extractPermissions(token);

                if (role == null || role.isBlank()) {
                    log.debug("Rejected access token without role [{}]", request.getRequestURI());
                } else {
                    log.debug("Authenticated request for user '{}' (role={}) on tenant '{}' [{}]",
                            username, role, schema, request.getRequestURI());

                    TenantContext.setCurrentTenant(schema);

                    var principal = new UserPrincipal(
                            userId, username, role, new java.util.HashSet<>(permissions));

                    List<org.springframework.security.core.GrantedAuthority> authorities =
                            new java.util.ArrayList<>();
                    authorities.add(() -> "ROLE_" + role);
                    permissions.forEach(permission -> authorities.add(() -> permission));

                    var authToken = new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            authorities
                    );

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } else {
                log.debug("Rejected invalid/non-access JWT [{}]", request.getRequestURI());
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }
}
