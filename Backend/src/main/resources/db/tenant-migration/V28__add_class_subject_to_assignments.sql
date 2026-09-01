-- Introduce class-scoped assignment targeting without breaking existing assignment rows.
-- Existing rows remain legacy (class_subject_id NULL) until explicitly reconciled.
ALTER TABLE assignments
    ADD COLUMN class_subject_id BIGINT;

ALTER TABLE assignments
    ADD CONSTRAINT fk_assignment_class_subject
        FOREIGN KEY (class_subject_id)
        REFERENCES class_subjects(id);

CREATE INDEX idx_assignment_class_subject
    ON assignments(class_subject_id);
