package com.collegeerp.Backend.config;

import com.collegeerp.Backend.tenant.TenantContext;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;

/**
 * Prevents cache collisions between tenant schemas.
 * Cached tenant-scoped services must use this generator so the same entity ID
 * in two different tenant schemas never resolves to the same Redis key.
 */
@Configuration
public class TenantAwareKeyGenerator {

    @Bean("tenantAwareKeyGenerator")
    public KeyGenerator tenantAwareKeyGenerator() {
        return (Object target, Method method, Object... params) -> {
            String tenant = TenantContext.getCurrentTenant();
            if (tenant == null || tenant.isBlank()) {
                throw new IllegalStateException(
                        "Tenant context is required for a tenant-scoped cache operation");
            }

            Object[] keyParts = new Object[params.length + 1];
            keyParts[0] = tenant;
            System.arraycopy(params, 0, keyParts, 1, params.length);
            return new SimpleKey(keyParts);
        };
    }
}
