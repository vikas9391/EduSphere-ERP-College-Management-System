-- Adds the Role + Permission system described in the "Role & Permission Management"
-- feature plan: a per-tenant Role now carries a set of granular permissions instead of
-- being just a name, and every User can be forced to change an admin-issued password
-- on first login.

ALTER TABLE roles
    ADD COLUMN is_system_role BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE users
    ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT true;

-- Existing accounts (admins who registered before this migration) already know their
-- own password - don't force them through the change-password screen retroactively.
UPDATE users SET must_change_password = false;

CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission VARCHAR(60) NOT NULL,
    PRIMARY KEY (role_id, permission)
);

-- Every existing ADMIN role (seeded fresh per tenant in
-- TenantProvisioningService#seedAdminUser, one row per tenant - not the unused
-- COLLEGE_ADMIN/FACULTY/STUDENT/PARENT rows from V1) becomes a locked system role and
-- is granted every permission that exists today, so `hasRole('ADMIN')` checks already
-- in the codebase (TeacherController, etc.) keep working unmodified alongside the new
-- `hasAuthority(...)` checks.
UPDATE roles SET is_system_role = true WHERE name = 'ADMIN';

INSERT INTO role_permissions (role_id, permission)
SELECT r.id, p.permission
FROM roles r
CROSS JOIN (VALUES
    ('CREATE_TEACHER'), ('EDIT_TEACHER'), ('DELETE_TEACHER'), ('VIEW_TEACHER'), ('VIEW_TEACHER_PROGRESS'),
    ('CREATE_STUDENT'), ('EDIT_STUDENT'), ('DELETE_STUDENT'), ('VIEW_STUDENT'),
    ('CREATE_DEPARTMENT'), ('EDIT_DEPARTMENT'), ('DELETE_DEPARTMENT'), ('VIEW_DEPARTMENT'),
    ('CREATE_COURSE'), ('EDIT_COURSE'), ('DELETE_COURSE'), ('VIEW_COURSE'),
    ('CREATE_SUBJECT'), ('EDIT_SUBJECT'), ('DELETE_SUBJECT'), ('VIEW_SUBJECT'),
    ('MANAGE_ENROLLMENT'), ('VIEW_ENROLLMENT'), ('MANAGE_ATTENDANCE'), ('VIEW_ATTENDANCE_REPORTS'),
    ('MANAGE_ASSIGNMENTS'), ('VIEW_ASSIGNMENTS'), ('MANAGE_EXAMS'), ('MANAGE_MARKS'), ('VIEW_RESULTS'),
    ('CREATE_ROLE'), ('EDIT_ROLE'), ('DELETE_ROLE'), ('ASSIGN_ROLE'),
    ('CREATE_USER'), ('EDIT_USER'), ('DELETE_USER'), ('VIEW_USER')
) AS p(permission)
WHERE r.name = 'ADMIN';
