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
 * Extracts and validates the JWT on every request, populates the Spring Security
 * context with a {@link UserPrincipal}, and sets the resolved tenant schema for the
 * duration of the request via {@link TenantContext}. Always clears the tenant context
 * afterwards, even if downstream processing throws.
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
            String token = authHeader.substring(BEARER_PREFIX.length());

            if (jwtService.isTokenValid(token)) {
                String schema = jwtService.extractSchema(token);
                String username = jwtService.extractUsername(token);
                String role = jwtService.extractClaims(token).get("role", String.class);
                Long userId = jwtService.extractUserId(token);

                log.debug("Authenticated request for user '{}' (role={}) on tenant '{}' [{}]",
                        username, role, schema, request.getRequestURI());

                TenantContext.setCurrentTenant(schema);

                var principal = new UserPrincipal(userId, username, role);

                var authToken = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(() -> "ROLE_" + role)
                );

                SecurityContextHolder.getContext().setAuthentication(authToken);
            } else {
                log.debug("Rejected request with invalid/expired JWT [{}]", request.getRequestURI());
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
