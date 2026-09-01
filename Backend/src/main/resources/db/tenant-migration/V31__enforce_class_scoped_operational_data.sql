-- Finalize the class-scoped academic relationship migration.
-- Fail fast if any legacy operational row still needs reconciliation.
-- This migration intentionally does not delete legacy data.

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM assignments WHERE class_subject_id IS NULL) THEN
        RAISE EXCEPTION 'Cannot finalize migration: assignments without class_subject_id remain';
    END IF;

    IF EXISTS (SELECT 1 FROM exam_schedules WHERE class_subject_id IS NULL) THEN
        RAISE EXCEPTION 'Cannot finalize migration: exam_schedules without class_subject_id remain';
    END IF;

    IF EXISTS (SELECT 1 FROM marks WHERE class_enrollment_id IS NULL) THEN
        RAISE EXCEPTION 'Cannot finalize migration: marks without class_enrollment_id remain';
    END IF;

    IF EXISTS (SELECT 1 FROM attendance WHERE enrollment_id IS NOT NULL AND class_enrollment_id IS NULL) THEN
        RAISE EXCEPTION 'Cannot finalize migration: legacy attendance without class_enrollment_id remains';
    END IF;
END $$;

ALTER TABLE assignments
    ALTER COLUMN class_subject_id SET NOT NULL;

ALTER TABLE exam_schedules
    ALTER COLUMN class_subject_id SET NOT NULL;

ALTER TABLE marks
    ALTER COLUMN class_enrollment_id SET NOT NULL;

ALTER TABLE attendance
    DROP CONSTRAINT IF EXISTS chk_attendance_enrollment_source;

ALTER TABLE attendance
    ALTER COLUMN class_enrollment_id SET NOT NULL;

-- Legacy enrollment_id is retained temporarily for historical compatibility.
-- It is intentionally not dropped until a later cleanup migration.
CREATE INDEX IF NOT EXISTS idx_attendance_class_enrollment
    ON attendance(class_enrollment_id);
