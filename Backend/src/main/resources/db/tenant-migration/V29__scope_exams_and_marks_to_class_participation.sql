-- Scope new exam schedules and marks to the exact taught class subject / student participation.
-- Existing rows remain valid with NULL compatibility columns until reconciliation is complete.

ALTER TABLE exam_schedules
    ADD COLUMN class_subject_id BIGINT;

ALTER TABLE exam_schedules
    ADD CONSTRAINT fk_exam_schedule_class_subject
        FOREIGN KEY (class_subject_id)
        REFERENCES class_subjects(id);

ALTER TABLE exam_schedules
    DROP CONSTRAINT IF EXISTS uk_exam_subject;

CREATE UNIQUE INDEX uq_exam_schedule_class_subject
    ON exam_schedules(exam_id, class_subject_id)
    WHERE class_subject_id IS NOT NULL;

CREATE UNIQUE INDEX uq_exam_schedule_legacy_subject
    ON exam_schedules(exam_id, subject_id)
    WHERE class_subject_id IS NULL;

CREATE INDEX idx_exam_schedule_class_subject
    ON exam_schedules(class_subject_id);

ALTER TABLE marks
    ADD COLUMN class_enrollment_id BIGINT;

ALTER TABLE marks
    ADD CONSTRAINT fk_marks_class_enrollment
        FOREIGN KEY (class_enrollment_id)
        REFERENCES class_enrollments(id);

CREATE INDEX idx_marks_class_enrollment
    ON marks(class_enrollment_id);
