-- Enrollment is a term-scoped academic record. The original V8 constraint
-- prevented a student from enrolling in the same subject again in a later
-- academic year/semester, even though those fields are explicitly part of the
-- enrollment record.
--
-- Existing databases containing duplicate rows for the same student/subject/
-- academic year/semester must be cleaned up before this migration can apply.

ALTER TABLE enrollments
    DROP CONSTRAINT IF EXISTS uk_student_subject;

ALTER TABLE enrollments
    ADD CONSTRAINT uk_student_subject_term
        UNIQUE (student_id, subject_id, academic_year, semester);
