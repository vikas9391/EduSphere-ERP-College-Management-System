-- Public-schema counterpart to tenant-migration's password_reset_tokens table,
-- scoped to the super_admins table (see V2__create_super_admins_table.sql) since a
-- super admin isn't tied to any tenant schema. This is run against the "public"
-- schema on every boot the same way V1-V3 are (spring.flyway.schemas=public).
CREATE TABLE super_admin_password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_super_admin_password_reset_tokens_token ON super_admin_password_reset_tokens(token);
