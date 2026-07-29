-- V1 seeded four placeholder roles (COLLEGE_ADMIN, FACULTY, STUDENT, PARENT) before the
-- Role & Permission Management feature existed. They were never actually used: every
-- tenant's real admin account uses the 'ADMIN' role seeded separately by
-- TenantProvisioningService#seedAdminUser (and locked as a system role by V19), and
-- Teacher/Student accounts live in their own tables entirely outside this
-- Role/Permission system.
--
-- Left as-is, these four rows aren't marked is_system_role, so RoleController's normal
-- CRUD lets a tenant admin rename, re-permission, or delete them like any other custom
-- role - dead rows masquerading as real ones in the Roles screen.
--
-- Deleted rather than merely locked, since they're genuinely unused - guarded by
-- NOT EXISTS so a tenant that somehow does have a user pointed at one of them keeps
-- that row untouched instead of failing the migration or orphaning the user.
DELETE FROM roles r
WHERE r.name IN ('COLLEGE_ADMIN', 'FACULTY', 'STUDENT', 'PARENT')
  AND NOT EXISTS (
      SELECT 1 FROM users u WHERE u.role_id = r.id
  );
