-- Fresh-start assignment schema: every assignment targets the exact taught
-- subject instance represented by ClassSubject.
ALTER TABLE assignments
    ADD COLUMN class_subject_id BIGINT NOT NULL;

ALTER TABLE assignments
    ADD CONSTRAINT fk_assignment_class_subject
        FOREIGN KEY (class_subject_id)
        REFERENCES class_subjects(id);

CREATE INDEX idx_assignment_class_subject
    ON assignments(class_subject_id);
