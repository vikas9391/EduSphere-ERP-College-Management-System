-- Safely backfill legacy operational rows into the ClassSubject/ClassEnrollment model.
-- Only unambiguous relationships are migrated. Anything that cannot be mapped safely remains
-- on the compatibility columns and is recorded for manual reconciliation.

CREATE TABLE IF NOT EXISTS academic_relationship_migration_audit (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_academic_relationship_migration_audit
        UNIQUE (entity_type, entity_id, reason)
);

-- Attendance: Enrollment -> Student + Subject -> exactly one matching ClassEnrollment.
-- Do not create a duplicate when the migrated class attendance row already exists for the date.
WITH attendance_candidates AS (
    SELECT a.id AS attendance_id, MIN(ce.id) AS class_enrollment_id
    FROM attendance a
    JOIN enrollments e ON e.id = a.enrollment_id
    JOIN class_enrollments ce ON ce.student_id = e.student_id
    JOIN class_subjects cs
      ON cs.id = ce.class_subject_id
     AND cs.subject_id = e.subject_id
    WHERE a.class_enrollment_id IS NULL
      AND a.enrollment_id IS NOT NULL
    GROUP BY a.id
    HAVING COUNT(ce.id) = 1
), safe_attendance_candidates AS (
    SELECT c.attendance_id, c.class_enrollment_id
    FROM attendance_candidates c
    JOIN attendance a ON a.id = c.attendance_id
    WHERE NOT EXISTS (
        SELECT 1
        FROM attendance existing
        WHERE existing.class_enrollment_id = c.class_enrollment_id
          AND existing.attendance_date = a.attendance_date
    )
)
UPDATE attendance a
SET class_enrollment_id = c.class_enrollment_id
FROM safe_attendance_candidates c
WHERE a.id = c.attendance_id;

-- Assignments: formal Subject + assigned teacher -> exactly one matching ClassSubject.
UPDATE assignments a
SET class_subject_id = (
    SELECT MIN(cs.id)
    FROM class_subjects cs
    WHERE cs.subject_id = a.subject_id
      AND cs.teacher_id = a.teacher_id
)
WHERE a.class_subject_id IS NULL
  AND 1 = (
      SELECT COUNT(*)
      FROM class_subjects cs
      WHERE cs.subject_id = a.subject_id
        AND cs.teacher_id = a.teacher_id
  );

-- Exam schedules: formal Subject -> exactly one matching ClassSubject.
UPDATE exam_schedules es
SET class_subject_id = (
    SELECT MIN(cs.id)
    FROM class_subjects cs
    WHERE cs.subject_id = es.subject_id
)
WHERE es.class_subject_id IS NULL
  AND 1 = (
      SELECT COUNT(*)
      FROM class_subjects cs
      WHERE cs.subject_id = es.subject_id
  );

-- Marks: once the schedule has an exact ClassSubject, attach the student's exact participation.
UPDATE marks m
SET class_enrollment_id = ce.id
FROM exam_schedules es
JOIN class_enrollments ce ON ce.class_subject_id = es.class_subject_id
WHERE m.exam_schedule_id = es.id
  AND ce.student_id = m.student_id
  AND es.class_subject_id IS NOT NULL
  AND m.class_enrollment_id IS NULL;

-- Record anything still requiring manual reconciliation. No data is deleted.
INSERT INTO academic_relationship_migration_audit(entity_type, entity_id, reason)
SELECT 'ATTENDANCE', a.id, 'No unique ClassEnrollment mapping or migrated row already exists'
FROM attendance a
WHERE a.class_enrollment_id IS NULL
  AND a.enrollment_id IS NOT NULL
ON CONFLICT DO NOTHING;

INSERT INTO academic_relationship_migration_audit(entity_type, entity_id, reason)
SELECT 'ASSIGNMENT', a.id, 'No unique ClassSubject mapping for subject and teacher'
FROM assignments a
WHERE a.class_subject_id IS NULL
ON CONFLICT DO NOTHING;

INSERT INTO academic_relationship_migration_audit(entity_type, entity_id, reason)
SELECT 'EXAM_SCHEDULE', es.id, 'No unique ClassSubject mapping for subject'
FROM exam_schedules es
WHERE es.class_subject_id IS NULL
ON CONFLICT DO NOTHING;

INSERT INTO academic_relationship_migration_audit(entity_type, entity_id, reason)
SELECT 'MARKS', m.id, 'Schedule is class-scoped but student has no matching ClassEnrollment'
FROM marks m
JOIN exam_schedules es ON es.id = m.exam_schedule_id
WHERE es.class_subject_id IS NOT NULL
  AND m.class_enrollment_id IS NULL
ON CONFLICT DO NOTHING;
