package com.collegeerp.Backend.config;

import com.collegeerp.Backend.tenant.TenantContext;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Tells Hibernate which schema to use for the current thread's session, based on
 * whatever tenant was resolved by JwtAuthFilter / AuthController and stashed in
 * TenantContext. Falls back to the "public" schema for requests with no tenant
 * (e.g. tenant registration endpoints).
 */
@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {

    private static final Logger log = LoggerFactory.getLogger(TenantIdentifierResolver.class);
    private static final String DEFAULT_SCHEMA = "public";

    @Override
    public String resolveCurrentTenantIdentifier() {
        String tenant = TenantContext.getCurrentTenant();
        log.trace("Resolving current tenant schema: {}", tenant);
        return tenant != null ? tenant : DEFAULT_SCHEMA;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
