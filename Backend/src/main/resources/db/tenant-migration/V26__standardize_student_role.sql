-- Standardize the student role to the singular canonical value STUDENT.
-- If an older tenant contains STUDENTS, move its users/permissions to STUDENT
-- before removing the legacy role. Fresh tenants already get STUDENT from V1.
DO $$
DECLARE
    student_role_id BIGINT;
    legacy_role_id BIGINT;
BEGIN
    SELECT id INTO student_role_id FROM roles WHERE name = 'STUDENT' LIMIT 1;
    SELECT id INTO legacy_role_id FROM roles WHERE name = 'STUDENTS' LIMIT 1;

    IF legacy_role_id IS NOT NULL AND student_role_id IS NULL THEN
        UPDATE roles SET name = 'STUDENT' WHERE id = legacy_role_id;
    ELSIF legacy_role_id IS NOT NULL AND student_role_id IS NOT NULL AND legacy_role_id <> student_role_id THEN
        UPDATE users SET role_id = student_role_id WHERE role_id = legacy_role_id;

        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'role_permissions') THEN
            INSERT INTO role_permissions (role_id, permission)
            SELECT student_role_id, permission
            FROM role_permissions
            WHERE role_id = legacy_role_id
            ON CONFLICT (role_id, permission) DO NOTHING;

            DELETE FROM role_permissions WHERE role_id = legacy_role_id;
        END IF;

        DELETE FROM roles WHERE id = legacy_role_id;
    END IF;
END $$;