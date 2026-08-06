-- Eliminates the separate "teachers" table entirely. Teacher becomes just a role
-- (TEACHER) on the same "users" table every other staff account already lives on;
-- teacher-only profile fields move onto "users" as nullable columns (null for every
-- non-teacher role). Every teacher_id FK across the schema now points at users.id.
--
-- DDL only, no data backfill - this project's DB is small/fresh enough that the
-- simplest path is to recreate it after this migration rather than migrate rows.

-- 1. Teacher-only profile columns on users (nullable - only ever set for TEACHER-role rows).
ALTER TABLE users ADD COLUMN employee_id VARCHAR(255) UNIQUE;
ALTER TABLE users ADD COLUMN phone VARCHAR(255);
ALTER TABLE users ADD COLUMN gender VARCHAR(255);
ALTER TABLE users ADD COLUMN qualification VARCHAR(255);
ALTER TABLE users ADD COLUMN specialization VARCHAR(255);
ALTER TABLE users ADD COLUMN experience INTEGER;
ALTER TABLE users ADD COLUMN joining_date DATE;

-- 2. Seed the built-in "TEACHER" role, same permission set as before.
INSERT INTO roles (name, description, is_system_role)
SELECT 'TEACHER', 'Teaching staff', true
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'TEACHER');

INSERT INTO role_permissions (role_id, permission)
SELECT r.id, p.permission
FROM roles r
CROSS JOIN (VALUES
    ('VIEW_STUDENT'), ('VIEW_SUBJECT'), ('VIEW_COURSE'), ('VIEW_DEPARTMENT'),
    ('VIEW_ENROLLMENT'), ('MANAGE_ATTENDANCE'), ('VIEW_ATTENDANCE_REPORTS'),
    ('MANAGE_ASSIGNMENTS'), ('VIEW_ASSIGNMENTS'), ('MANAGE_MARKS'), ('VIEW_RESULTS')
) AS p(permission)
WHERE r.name = 'TEACHER'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission = p.permission
  );

-- 3. Repoint every teacher_id / invigilator_id FK at users(id) instead of teachers(id).
--    Tables are expected to be empty at this point (fresh/small dev DB) - if any of
--    these still hold rows referencing old teachers.id values, truncate them first.
ALTER TABLE subjects DROP CONSTRAINT IF EXISTS fk_subject_teacher;
ALTER TABLE subjects ADD CONSTRAINT fk_subject_teacher FOREIGN KEY (teacher_id) REFERENCES users(id);

ALTER TABLE school_classes DROP CONSTRAINT IF EXISTS fk_class_teacher;
ALTER TABLE school_classes ADD CONSTRAINT fk_class_teacher FOREIGN KEY (teacher_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE class_subjects DROP CONSTRAINT IF EXISTS fk_clssub_teacher;
ALTER TABLE class_subjects ADD CONSTRAINT fk_clssub_teacher FOREIGN KEY (teacher_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE assignments DROP CONSTRAINT IF EXISTS fk_assignment_teacher;
ALTER TABLE assignments ADD CONSTRAINT fk_assignment_teacher FOREIGN KEY (teacher_id) REFERENCES users(id);

ALTER TABLE exam_schedules DROP CONSTRAINT IF EXISTS fk_schedule_invigilator;
ALTER TABLE exam_schedules ADD CONSTRAINT fk_schedule_invigilator FOREIGN KEY (invigilator_id) REFERENCES users(id);

-- 4. Drop the old teachers table - its credentials belong on users now (see step 1's
--    profile columns), and every FK that pointed at it was repointed in step 3.
DROP TABLE IF EXISTS teachers;
