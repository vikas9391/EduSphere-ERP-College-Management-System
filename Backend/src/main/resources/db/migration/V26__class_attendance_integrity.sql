-- Class-based attendance is keyed by class enrollment + date.
-- Keep the newest duplicate if older development/test data already contains duplicates.
DELETE FROM attendance a
WHERE a.class_enrollment_id IS NOT NULL
  AND a.id NOT IN (
      SELECT MAX(id)
      FROM attendance
      WHERE class_enrollment_id IS NOT NULL
      GROUP BY class_enrollment_id, attendance_date
  );

CREATE UNIQUE INDEX IF NOT EXISTS ux_attendance_class_enrollment_date
    ON attendance (class_enrollment_id, attendance_date)
    WHERE class_enrollment_id IS NOT NULL;

-- Class attendance must not be simultaneously attached to the legacy enrollment.
-- Existing rows are left untouched for backward compatibility; application writes
-- already choose exactly one relationship.
