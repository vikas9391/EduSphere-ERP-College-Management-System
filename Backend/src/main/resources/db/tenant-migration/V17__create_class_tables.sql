-- Lets a teacher set up a "class" (a batch/section they own) as a self-contained unit:
-- a name + academic year/semester, a roster of students, and a set of subjects for that
-- roster - without needing an admin to pre-create a Course, Subject, and one Enrollment
-- row per student per subject. Deliberately NOT linked to the existing courses table
-- (a class is a lighter-weight, teacher-owned grouping, not part of the formal
-- curriculum/Marks/Exam structure that Subject already serves) to avoid entangling this
-- with that more heavyweight, admin-managed system.

CREATE TABLE school_classes (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    academic_year VARCHAR(20) NOT NULL,
    semester INTEGER NOT NULL,
    -- Fixed cap on how many subjects this class may ever have. NULL = uncapped. Set at
    -- creation time by the owning teacher; enforced in ClassSubjectService.
    max_subjects INTEGER,
    teacher_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_class_teacher
        FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE CASCADE
);

-- The class roster. A student can be in many classes and a class has many students.
CREATE TABLE class_students (
    id BIGSERIAL PRIMARY KEY,
    school_class_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cs_class
        FOREIGN KEY (school_class_id) REFERENCES school_classes(id) ON DELETE CASCADE,
    CONSTRAINT fk_cs_student
        FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT uq_class_student UNIQUE (school_class_id, student_id)
);

-- Subjects scoped to one class. subject_code is only unique within the class (not
-- globally, unlike the standalone subjects table), since each teacher's classes are
-- independent of one another.
CREATE TABLE class_subjects (
    id BIGSERIAL PRIMARY KEY,
    school_class_id BIGINT NOT NULL,
    subject_code VARCHAR(30) NOT NULL,
    subject_name VARCHAR(150) NOT NULL,
    credits INTEGER NOT NULL,
    teacher_id BIGINT NOT NULL,
    -- MANDATORY: every current and future roster student is auto-enrolled.
    -- ELECTIVE: students opt themselves in/out; never auto-enrolled.
    enrollment_mode VARCHAR(20) NOT NULL DEFAULT 'MANDATORY',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_clssub_class
        FOREIGN KEY (school_class_id) REFERENCES school_classes(id) ON DELETE CASCADE,
    CONSTRAINT fk_clssub_teacher
        FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE CASCADE,
    CONSTRAINT uq_class_subject_code UNIQUE (school_class_id, subject_code),
    CONSTRAINT chk_enrollment_mode CHECK (enrollment_mode IN ('MANDATORY', 'ELECTIVE'))
);

-- The actual student <-> class-subject connection. Rows are created either
-- automatically (MANDATORY subjects, or a student newly added to a class that already
-- has MANDATORY subjects) or by the student themself (ELECTIVE subjects) - `source`
-- records which, for auditing/UI purposes.
CREATE TABLE class_enrollments (
    id BIGSERIAL PRIMARY KEY,
    class_subject_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    source VARCHAR(10) NOT NULL,
    enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ce_subject
        FOREIGN KEY (class_subject_id) REFERENCES class_subjects(id) ON DELETE CASCADE,
    CONSTRAINT fk_ce_student
        FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT uq_class_enrollment UNIQUE (class_subject_id, student_id),
    CONSTRAINT chk_enrollment_source CHECK (source IN ('AUTO', 'SELF'))
);
