-- Fee-management permissions for tenants created before V30/V31.
INSERT INTO role_permissions (role_id, permission)
SELECT r.id, p.permission
FROM roles r
CROSS JOIN (VALUES ('MANAGE_FEES'), ('VIEW_FEES')) AS p(permission)
WHERE r.name = 'ADMIN'
ON CONFLICT (role_id, permission) DO NOTHING;

-- Teachers may view their students' fee status but cannot record or assign payments.
INSERT INTO role_permissions (role_id, permission)
SELECT r.id, 'VIEW_FEES'
FROM roles r
WHERE r.name = 'TEACHER'
ON CONFLICT (role_id, permission) DO NOTHING;
