package com.collegeerp.Backend.tenant.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.collegeerp.Backend.common.Role;
import com.collegeerp.Backend.common.RoleRepository;
import com.collegeerp.Backend.common.User;
import com.collegeerp.Backend.common.UserRepository;
import com.collegeerp.Backend.common.exception.DuplicateResourceException;
import com.collegeerp.Backend.common.exception.ResourceNotFoundException;
import com.collegeerp.Backend.common.exception.TenantProvisioningException;
import com.collegeerp.Backend.tenant.TenantContext;
import com.collegeerp.Backend.tenant.dto.TenantRegistrationRequest;
import com.collegeerp.Backend.tenant.dto.TenantRegistrationResponse;
import com.collegeerp.Backend.tenant.dto.TenantSubscriptionUpdateRequest;
import com.collegeerp.Backend.tenant.entity.Tenant;
import com.collegeerp.Backend.tenant.repository.TenantRepository;

/**
 * Provisions a brand-new tenant: creates its Postgres schema, runs the tenant-scoped
 * Flyway migrations (via {@link TenantSchemaMigrator}, shared with
 * {@link TenantSchemaStartupMigrator} which re-runs the same migration against every
 * EXISTING tenant on application startup), seeds an ADMIN role + admin user inside
 * that schema, then records the tenant in the public "tenants" table.
 * <p>
 * NOTE on transactionality: this process necessarily spans the public schema and a
 * freshly-created tenant schema using raw JDBC (schema creation, Flyway) plus two
 * separate JPA persistence operations, so it cannot be wrapped in a single
 * {@code @Transactional} boundary. If the admin-user save or the final tenant-row save
 * fails after the schema/migration already succeeded, the schema is left behind
 * un-registered in the "tenants" table; this is logged clearly so it can be cleaned up
 * or retried manually. A full saga/compensating-transaction implementation is a
 * larger architectural change tracked for a future pass, not part of this refactor.
 */
@Service
public class TenantProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(TenantProvisioningService.class);
    private static final String ADMIN_ROLE = "ADMIN";

    private final TenantRepository tenantRepository;
    private final TenantSchemaMigrator schemaMigrator;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public TenantProvisioningService(
            TenantRepository tenantRepository,
            TenantSchemaMigrator schemaMigrator,
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.tenantRepository = tenantRepository;
        this.schemaMigrator = schemaMigrator;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public TenantRegistrationResponse register(TenantRegistrationRequest request) {

        String subdomain = request.getSubdomain().trim().toLowerCase();

        if (tenantRepository.existsBySubdomain(subdomain)) {
            throw new DuplicateResourceException("Subdomain '" + subdomain + "' is already registered");
        }

        String schemaName = toSchemaName(subdomain);

        if (tenantRepository.existsBySchemaName(schemaName)) {
            throw new DuplicateResourceException("A tenant already maps to schema '" + schemaName + "'");
        }

        Tenant tenant = provisionNewTenant(
                request.getCollegeName(),
                subdomain,
                schemaName,
                request.getAdminEmail(),
                request.getPassword()
        );

        log.info("Provisioned new tenant '{}' (schema={}, subdomain={})",
                tenant.getName(), tenant.getSchemaName(), tenant.getSubdomain());

        return new TenantRegistrationResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getSchemaName(),
                "College registered successfully"
        );
    }

    private String toSchemaName(String subdomain) {
        return subdomain.replaceAll("[^a-z0-9]", "_");
    }

    /** Suspends or reactivates a college. Reversible - no data is touched. */
    public Tenant updateStatus(UUID tenantId, boolean isActive) {
        Tenant tenant = getOrThrow(tenantId);
        tenant.setActive(isActive);
        tenant = tenantRepository.save(tenant);
        log.info("Tenant '{}' (schema={}) marked {}", tenant.getName(), tenant.getSchemaName(),
                isActive ? "ACTIVE" : "SUSPENDED");
        return tenant;
    }

    /** Updates subscription plan/status/expiry. Bookkeeping only - does not affect login access. */
    public Tenant updateSubscription(UUID tenantId, TenantSubscriptionUpdateRequest request) {
        Tenant tenant = getOrThrow(tenantId);
        tenant.setSubscriptionPlan(request.plan().trim());
        tenant.setSubscriptionStatus(request.status().trim().toUpperCase());
        tenant.setSubscriptionExpiresAt(request.expiresAt());
        tenant = tenantRepository.save(tenant);
        log.info("Tenant '{}' subscription updated to plan={} status={} expiresAt={}",
                tenant.getName(), tenant.getSubscriptionPlan(), tenant.getSubscriptionStatus(),
                tenant.getSubscriptionExpiresAt());
        return tenant;
    }

    /**
     * Irreversibly deletes a college: drops its entire Postgres schema (see
     * {@link TenantSchemaMigrator#dropSchema}) and removes its row from the public
     * {@code tenants} table. Every student, teacher, course, and record that college ever
     * had is gone with it - there is no undo short of a database backup. The controller
     * layer is expected to have already gotten explicit confirmation before calling this.
     */
    public void deleteTenant(UUID tenantId) {
        Tenant tenant = getOrThrow(tenantId);
        schemaMigrator.dropSchema(tenant.getSchemaName());
        tenantRepository.delete(tenant);
        log.warn("Deleted tenant '{}' (schema={}, subdomain={}) - schema dropped, all data gone",
                tenant.getName(), tenant.getSchemaName(), tenant.getSubdomain());
    }

    private Tenant getOrThrow(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> ResourceNotFoundException.of("College", tenantId));
    }

    private Tenant provisionNewTenant(String collegeName, String subdomain, String schemaName,
                                       String adminEmail, String password) {

        schemaMigrator.migrateSchema(schemaName);
        seedAdminUser(schemaName, collegeName, adminEmail, password);

        Tenant tenant = Tenant.builder()
                .name(collegeName)
                .schemaName(schemaName)
                .subdomain(subdomain)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        return tenantRepository.save(tenant);
    }

    private void seedAdminUser(String schemaName, String collegeName, String adminEmail, String password) {
        TenantContext.setCurrentTenant(schemaName);
        try {
            Role adminRole = new Role();
            adminRole.setName(ADMIN_ROLE);
            adminRole.setDescription("College Administrator");
            adminRole = roleRepository.save(adminRole);

            User admin = User.builder()
                    .email(adminEmail)
                    .passwordHash(passwordEncoder.encode(password))
                    .firstName("Admin")
                    .lastName(collegeName)
                    .role(adminRole)
                    .isActive(true)
                    .isEmailVerified(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            userRepository.save(admin);

        } catch (Exception e) {
            throw new TenantProvisioningException(
                    "Schema '" + schemaName + "' was created and migrated, but seeding the admin user failed. "
                            + "The schema exists but no tenant record was saved - manual cleanup or retry may be required.",
                    e);
        } finally {
            TenantContext.clear();
        }
    }
}