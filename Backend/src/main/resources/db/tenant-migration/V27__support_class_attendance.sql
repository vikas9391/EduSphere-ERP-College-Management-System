-- Finalize the class-scoped attendance relationship after class tables exist.
ALTER TABLE attendance
    ADD CONSTRAINT fk_attendance_class_enrollment
    FOREIGN KEY (class_enrollment_id)
    REFERENCES class_enrollments(id)
    ON DELETE CASCADE;

CREATE UNIQUE INDEX uk_attendance_class_enrollment_date
    ON attendance(class_enrollment_id, attendance_date);