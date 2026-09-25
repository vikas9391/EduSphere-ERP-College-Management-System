-- Optional bridge between a teacher-owned ClassSubject (see V17) and the formal
-- curriculum Subject table. Nullable on purpose: a class-subject can stay a purely
-- informal grouping (e.g. an ELECTIVE study group with no official backing) and this
-- column is simply left null - nothing about existing class_subjects behavior changes.
--
-- Marks and exam schedules are class-scoped. The exam schedule points to a ClassSubject,
-- and MarksService resolves eligibility through that ClassSubject's ClassEnrollment
-- records. This link only identifies the corresponding formal curriculum Subject for
-- reporting and display; it is not an alternate enrollment source.
ALTER TABLE class_subjects ADD COLUMN subject_id BIGINT NULL;

ALTER TABLE class_subjects ADD CONSTRAINT fk_clssub_subject
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE SET NULL;
