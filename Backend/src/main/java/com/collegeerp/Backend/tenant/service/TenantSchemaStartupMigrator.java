package com.collegeerp.Backend.tenant.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.collegeerp.Backend.tenant.entity.Tenant;
import com.collegeerp.Backend.tenant.repository.TenantRepository;

/**
 * Previously, a tenant's schema was only ever Flyway-migrated ONCE, at creation time
 * (see {@link TenantProvisioningService}). Any migration file added after a tenant was
 * already provisioned - e.g. V15 adding {@code teachers.password_hash} - would never
 * reach that tenant's schema, since nothing ever ran {@code migrate()} against it again.
 * This surfaced as a real production error: an already-provisioned college's schema was
 * missing the new column entirely, so teacher login failed with a raw SQL error instead
 * of a clean response.
 * <p>
 * This runner closes that gap: on every application startup, it re-runs
 * {@link TenantSchemaMigrator#migrateSchema(String)} against every tenant's schema.
 * This is safe and cheap to do on every boot - Flyway's migration history table means
 * already-applied migrations are skipped instantly; only genuinely new migration files
 * get applied to schemas that don't have them yet. For a college with many tenants this
 * adds some startup time proportional to tenant count; if that becomes a problem, this
 * could be moved to a manual admin-triggered endpoint instead, but the simplicity and
 * correctness of "always up to date after every boot" is worth it at this application's
 * current scale.
 */
@Component
public class TenantSchemaStartupMigrator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TenantSchemaStartupMigrator.class);

    private final TenantRepository tenantRepository;
    private final TenantSchemaMigrator schemaMigrator;

    public TenantSchemaStartupMigrator(TenantRepository tenantRepository, TenantSchemaMigrator schemaMigrator) {
        this.tenantRepository = tenantRepository;
        this.schemaMigrator = schemaMigrator;
    }

    @Override
    public void run(ApplicationArguments args) {
        var tenants = tenantRepository.findAll();

        if (tenants.isEmpty()) {
            log.info("No existing tenants found - skipping startup schema migration.");
            return;
        }

        log.info("Re-checking Flyway migrations for {} existing tenant schema(s)...", tenants.size());

        int failures = 0;
        for (Tenant tenant : tenants) {
            try {
                schemaMigrator.migrateSchema(tenant.getSchemaName());
            } catch (Exception e) {
                // One tenant's migration failing (e.g. a bad connection, or a genuinely
                // broken migration file) must not prevent the application from starting
                // or block every other tenant from being checked - log it clearly and
                // move on. That tenant's schema stays on whatever version it was already
                // on until this is fixed and the app restarted again.
                failures++;
                log.error("Failed to migrate schema '{}' for tenant '{}' - this tenant's schema "
                                + "may be missing recent migrations until this is resolved and the "
                                + "application is restarted.",
                        tenant.getSchemaName(), tenant.getName(), e);
            }
        }

        log.info("Startup tenant schema migration complete ({} succeeded, {} failed).",
                tenants.size() - failures, failures);
    }
}