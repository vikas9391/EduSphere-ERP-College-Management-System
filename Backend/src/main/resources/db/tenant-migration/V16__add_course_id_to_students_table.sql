-- Links each student to the course/programme they're admitted into. Previously a
-- student's course was only discoverable indirectly, by walking their enrollment rows
-- (student -> enrollment -> subject -> course), which meant there was no way to answer
-- "which course/batch is this student in" until they had at least one subject
-- enrollment. This column makes that a direct, always-available relationship, set at
-- admission time.
--
-- Nullable because existing students (rows created before this migration) have no
-- value to backfill from - an admin should assign their course via the students UI/API
-- after this migration runs. ON DELETE SET NULL rather than CASCADE/RESTRICT: deleting
-- a course shouldn't also delete every student ever admitted into it.
--
-- Like V15, this is picked up automatically for existing tenant schemas by
-- TenantSchemaStartupMigrator on the next application boot, and applied at creation
-- time for any new tenant provisioned after this change.

ALTER TABLE students ADD COLUMN course_id BIGINT;

ALTER TABLE students
    ADD CONSTRAINT fk_student_course
    FOREIGN KEY (course_id)
    REFERENCES courses(id)
    ON DELETE SET NULL;
