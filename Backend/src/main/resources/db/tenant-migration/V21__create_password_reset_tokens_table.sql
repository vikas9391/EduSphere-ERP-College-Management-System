-- Backs the "forgot password" flow for tenant-scoped accounts. One shared table for
-- all three account types (STAFF -> users, TEACHER -> teachers, STUDENT -> students)
-- rather than three separate tables, since the token/expiry/used lifecycle is
-- identical for all of them - account_type just says which repository owns the
-- matching email when the token is redeemed.
--
-- The public-schema super_admins table has its own equivalent, separate table
-- (see db/migration/V4__create_super_admin_password_reset_tokens_table.sql),
-- since a super admin isn't scoped to any tenant schema.
--
-- New tenant schemas pick this up automatically at creation
-- (TenantProvisioningService). Existing tenant schemas pick it up on the next
-- application boot via TenantSchemaStartupMigrator, which re-runs Flyway against
-- every already-provisioned schema on startup.
CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL,
    account_type VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_password_reset_tokens_token ON password_reset_tokens(token);
