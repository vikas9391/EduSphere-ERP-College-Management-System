ALTER TABLE attendance ALTER COLUMN enrollment_id DROP NOT NULL;
ALTER TABLE attendance ADD COLUMN class_enrollment_id BIGINT NULL;

ALTER TABLE attendance
    ADD CONSTRAINT fk_attendance_class_enrollment
    FOREIGN KEY (class_enrollment_id) REFERENCES class_enrollments(id) ON DELETE CASCADE;

ALTER TABLE attendance
    ADD CONSTRAINT chk_attendance_enrollment_source
    CHECK (enrollment_id IS NOT NULL OR class_enrollment_id IS NOT NULL);

CREATE UNIQUE INDEX uk_attendance_class_enrollment_date
    ON attendance(class_enrollment_id, attendance_date)
    WHERE class_enrollment_id IS NOT NULL;


-- Prevent duplicate attendance rows for the current class-based model.
CREATE UNIQUE INDEX IF NOT EXISTS uq_attendance_class_enrollment_date
    ON attendance(class_enrollment_id, attendance_date)
    WHERE class_enrollment_id IS NOT NULL;
