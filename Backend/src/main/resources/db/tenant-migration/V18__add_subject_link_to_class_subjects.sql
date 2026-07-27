-- Optional bridge between a teacher-owned ClassSubject (see V17) and the formal
-- curriculum Subject table. Nullable on purpose: a class-subject can stay a purely
-- informal grouping (e.g. an ELECTIVE study group with no official backing) and this
-- column is simply left null - nothing about existing class_subjects behavior changes.
--
-- When a teacher DOES link a class-subject to a real Subject, that class's roster
-- (class_enrollments) becomes an additional source of truth for who's eligible to have
-- marks entered against that Subject's exam schedules - see
-- MarksService.getEligibleStudents / MarksService.validateEligibility. This does not
-- replace the existing Enrollment-based flow; it only tightens eligibility where a link
-- exists.
ALTER TABLE class_subjects ADD COLUMN subject_id BIGINT NULL;

ALTER TABLE class_subjects ADD CONSTRAINT fk_clssub_subject
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE SET NULL;
