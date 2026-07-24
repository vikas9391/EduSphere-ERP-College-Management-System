-- Adds teacher login credentials. A new migration (not an edit to V4) because Flyway
-- validates checksums of already-applied migrations - editing V4 directly would break
-- migration history for any tenant schema provisioned before this change.
--
-- NOTE: tenant schemas are only Flyway-migrated once, at creation time, in
-- TenantProvisioningService.createSchemaAndMigrate(). There is currently no startup
-- routine that re-runs migrate() against already-provisioned tenant schemas, so this
-- migration will apply automatically for every NEW tenant created after this change,
-- but any tenant schema created BEFORE this change needs it applied manually, e.g.:
--   SET search_path TO "<existing_schema_name>";
--   ALTER TABLE teachers ADD COLUMN password_hash VARCHAR(255);
-- (existing teacher rows will have a NULL password_hash until an admin resets it via
-- the "Reset teacher password" flow, or re-saves the teacher with a new password.)

ALTER TABLE teachers ADD COLUMN password_hash VARCHAR(255);
