-- Role names are treated as case-insensitive by RoleService. Enforce the same
-- invariant at the database layer so direct SQL/import paths cannot create
-- effectively duplicate roles such as "Supervisor" and "SUPERVISOR".
CREATE UNIQUE INDEX uq_roles_name_lower ON roles (LOWER(name));
