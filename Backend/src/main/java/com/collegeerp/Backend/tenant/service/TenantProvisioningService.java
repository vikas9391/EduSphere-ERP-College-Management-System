package com.collegeerp.Backend.tenant.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.collegeerp.Backend.common.Permission;
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
    private static final String TEACHER_ROLE = "TEACHER";

    // Mirrors the permission set seeded for existing tenants by V22 -
    // TeacherService#createTeacher looks this role up by name, so every new tenant
    // needs it seeded here too, not just backfilled onto tenants that already existed.
    private static final java.util.Set<Permission> TEACHER_PERMISSIONS = java.util.Set.of(
            Permission.VIEW_STUDENT, Permission.VIEW_SUBJECT, Permission.VIEW_COURSE, Permission.VIEW_DEPARTMENT,
            Permission.VIEW_ENROLLMENT, Permission.MANAGE_ATTENDANCE, Permission.VIEW_ATTENDANCE_REPORTS,
            Permission.MANAGE_ASSIGNMENTS, Permission.VIEW_ASSIGNMENTS, Permission.MANAGE_MARKS, Permission.VIEW_RESULTS);

    private final TenantRepository tenantRepository;
    private final TenantSchemaMigrator schemaMigrator;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public TenantProvisioningService(
            TenantRepository tenantRepository,
            TenantSchemaMigrator schemaMigrator,
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder) {

        this.tenantRepository = tenantRepository;
        this.schemaMigrator = schemaMigrator;
        this.jdbcTemplate = jdbcTemplate;
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
                request.getPassword(),
                request.getSubscriptionPlan(),
                request.getSubscriptionExpiresAt()
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

    /**
     * Updates subscription plan/status/expiry. Enforcement now happens in
     * {@link SubscriptionExpiryService} - not here - so this method itself stays a plain
     * field update; it doesn't need to know about login access at all.
     */
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
                                       String adminEmail, String password,
                                       String subscriptionPlan, LocalDateTime subscriptionExpiresAt) {

        schemaMigrator.migrateSchema(schemaName);
        seedAdminUser(schemaName, collegeName, adminEmail, password);

        Tenant.TenantBuilder builder = Tenant.builder()
                .name(collegeName)
                .schemaName(schemaName)
                .subdomain(subdomain)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .subscriptionExpiresAt(subscriptionExpiresAt);

        // Only override the entity's "TRIAL" default if the caller actually supplied a
        // plan - an explicit blank field should fall back to trial, not to a literal
        // empty string being persisted.
        if (subscriptionPlan != null && !subscriptionPlan.isBlank()) {
            builder.subscriptionPlan(subscriptionPlan.trim().toUpperCase());
        }

        return tenantRepository.save(builder.build());
    }

    /**
     * Seeds the first tenant accounts directly with JDBC rather than JPA.
     *
     * Provisioning happens before a normal tenant request/session exists, so using the
     * multi-tenant JPA repositories here can bind a persistence context/connection at
     * the wrong point in the ThreadLocal tenant lifecycle. The schema is already known
     * and validated, so schema-qualified JDBC is deterministic and also makes retries
     * against an orphaned schema idempotent.
     */
    private void seedAdminUser(String schemaName, String collegeName, String adminEmail, String password) {
        String schema = quoteIdentifier(schemaName);
        String encodedPassword = passwordEncoder.encode(password);

        try {
            Long adminRoleId = upsertRole(
                    schema,
                    ADMIN_ROLE,
                    "College Administrator",
                    true
            );

            for (Permission permission : Permission.values()) {
                jdbcTemplate.update(
                        "INSERT INTO " + schema + ".role_permissions (role_id, permission) VALUES (?, ?) "
                                + "ON CONFLICT (role_id, permission) DO NOTHING",
                        adminRoleId,
                        permission.name()
                );
            }

            jdbcTemplate.update(
                    "INSERT INTO " + schema + ".users "
                            + "(email, password_hash, first_name, last_name, role_id, is_active, "
                            + "is_email_verified, must_change_password, created_at, updated_at) "
                            + "VALUES (?, ?, ?, ?, ?, true, true, false, now(), now()) "
                            + "ON CONFLICT (email) DO UPDATE SET "
                            + "password_hash = EXCLUDED.password_hash, "
                            + "first_name = EXCLUDED.first_name, "
                            + "last_name = EXCLUDED.last_name, "
                            + "role_id = EXCLUDED.role_id, "
                            + "is_active = true, "
                            + "is_email_verified = true, "
                            + "must_change_password = false, "
                            + "updated_at = now()",
                    adminEmail,
                    encodedPassword,
                    "Admin",
                    collegeName,
                    adminRoleId
            );

            Long teacherRoleId = upsertRole(
                    schema,
                    TEACHER_ROLE,
                    "Teaching staff",
                    true
            );

            for (Permission permission : TEACHER_PERMISSIONS) {
                jdbcTemplate.update(
                        "INSERT INTO " + schema + ".role_permissions (role_id, permission) VALUES (?, ?) "
                                + "ON CONFLICT (role_id, permission) DO NOTHING",
                        teacherRoleId,
                        permission.name()
                );
            }

        } catch (Exception e) {
            log.error("Failed seeding admin/teacher roles for tenant schema '{}'", schemaName, e);
            throw new TenantProvisioningException(
                    "Schema '" + schemaName + "' was created and migrated, but seeding the admin user failed. "
                            + "The schema exists but no tenant record was saved. The provisioning operation is retryable.",
                    e);
        }
    }

    private Long upsertRole(String schema, String roleName, String description, boolean systemRole) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO " + schema + ".roles (name, description, is_system_role) "
                        + "VALUES (?, ?, ?) "
                        + "ON CONFLICT (name) DO UPDATE SET "
                        + "description = EXCLUDED.description, "
                        + "is_system_role = EXCLUDED.is_system_role "
                        + "RETURNING id",
                Long.class,
                roleName,
                description,
                systemRole
        );
    }

    private String quoteIdentifier(String schemaName) {
        // schemaName is generated from the validated subdomain and contains only
        // lowercase letters, digits and underscores at this point.
        if (!schemaName.matches("[a-z0-9_]{1,63}")) {
            throw new IllegalArgumentException("Invalid tenant schema identifier");
        }
        return "\"" + schemaName + "\"";
    }

}