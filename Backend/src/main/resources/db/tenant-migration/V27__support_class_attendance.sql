-- Fresh-start attendance schema: attendance belongs only to the exact
-- student + taught-subject relationship represented by ClassEnrollment.
ALTER TABLE attendance
    DROP CONSTRAINT IF EXISTS uk_attendance;

ALTER TABLE attendance
    DROP CONSTRAINT IF EXISTS fk_attendance_enrollment;

ALTER TABLE attendance
    DROP COLUMN IF EXISTS enrollment_id;

ALTER TABLE attendance
    ADD COLUMN class_enrollment_id BIGINT NOT NULL;

ALTER TABLE attendance
    ADD CONSTRAINT fk_attendance_class_enrollment
    FOREIGN KEY (class_enrollment_id) REFERENCES class_enrollments(id) ON DELETE CASCADE;

CREATE UNIQUE INDEX uk_attendance_class_enrollment_date
    ON attendance(class_enrollment_id, attendance_date);
