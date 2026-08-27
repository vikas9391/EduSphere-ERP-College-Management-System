-- Demo academic data for local/testing tenants.
-- Safe to run on the existing tenant database: all rows use stable demo
-- emails/codes and are inserted only when they do not already exist.
-- Demo login passwords:
--   teacher.demo@edusphere.test / password
--   student.demo@edusphere.test / password
--
-- This migration intentionally does NOT create or replace a database.

DO $$
DECLARE
    teacher_role_id BIGINT;
    student_role_id BIGINT;
    teacher_id BIGINT;
    department_id BIGINT;
    course_id BIGINT;
    subject1_id BIGINT;
    subject2_id BIGINT;
    class_id BIGINT;
    student_id BIGINT;
    enrollment1_id BIGINT;
BEGIN
    SELECT id INTO teacher_role_id FROM roles WHERE name = 'TEACHER' LIMIT 1;
    IF teacher_role_id IS NULL THEN
        INSERT INTO roles (name, description, is_system_role)
        VALUES ('TEACHER', 'Teacher role', false)
        RETURNING id INTO teacher_role_id;
    END IF;

    SELECT id INTO student_role_id FROM roles WHERE name = 'STUDENT' LIMIT 1;
    IF student_role_id IS NULL THEN
        INSERT INTO roles (name, description, is_system_role)
        VALUES ('STUDENT', 'Student role', false)
        RETURNING id INTO student_role_id;
    END IF;

    SELECT id INTO teacher_id FROM users WHERE email = 'teacher.demo@edusphere.test' LIMIT 1;
    IF teacher_id IS NULL THEN
        INSERT INTO users
            (email, password_hash, first_name, last_name, role_id, is_active,
             is_email_verified, must_change_password, employee_id, created_at, updated_at)
        VALUES
            ('teacher.demo@edusphere.test',
             '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
             'Demo', 'Teacher', teacher_role_id, true, true, false,
             'DEMO-T001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        RETURNING id INTO teacher_id;
    ELSE
        UPDATE users
           SET role_id = teacher_role_id, is_active = true,
               must_change_password = false
         WHERE id = teacher_id;
    END IF;

    -- Courses require a department. Create/reuse a stable demo department first.
    SELECT id INTO department_id FROM departments WHERE code = 'DEMO-CSE' LIMIT 1;
    IF department_id IS NULL THEN
        INSERT INTO departments (code, name, hod_name, description, created_at)
        VALUES ('DEMO-CSE', 'Demo Computer Science', 'Demo HOD',
                'Demo department for ERP attendance testing', CURRENT_TIMESTAMP)
        RETURNING id INTO department_id;
    END IF;

    SELECT id INTO course_id FROM courses WHERE course_code = 'DEMO-BCA' LIMIT 1;
    IF course_id IS NULL THEN
        INSERT INTO courses
            (course_code, course_name, duration, description, department_id, created_at)
        VALUES
            ('DEMO-BCA', 'Demo Bachelor of Computer Applications', 3,
             'Demo course for ERP attendance testing', department_id, CURRENT_TIMESTAMP)
        RETURNING id INTO course_id;
    END IF;

    SELECT id INTO subject1_id FROM subjects WHERE subject_code = 'DEMO-JAVA' LIMIT 1;
    IF subject1_id IS NULL THEN
        INSERT INTO subjects
            (subject_code, subject_name, credits, semester, course_id, teacher_id, created_at)
        VALUES
            ('DEMO-JAVA', 'Java Programming', 4, 1, course_id, teacher_id, CURRENT_TIMESTAMP)
        RETURNING id INTO subject1_id;
    END IF;

    SELECT id INTO subject2_id FROM subjects WHERE subject_code = 'DEMO-DBMS' LIMIT 1;
    IF subject2_id IS NULL THEN
        INSERT INTO subjects
            (subject_code, subject_name, credits, semester, course_id, teacher_id, created_at)
        VALUES
            ('DEMO-DBMS', 'Database Management Systems', 4, 1, course_id, teacher_id, CURRENT_TIMESTAMP)
        RETURNING id INTO subject2_id;
    END IF;

    SELECT id INTO class_id FROM school_classes WHERE name = 'Demo BCA - Semester 1'
        AND academic_year = '2026-27' AND semester = 1 LIMIT 1;
    IF class_id IS NULL THEN
        INSERT INTO school_classes
            (name, academic_year, semester, max_subjects, teacher_id, created_at)
        VALUES
            ('Demo BCA - Semester 1', '2026-27', 1, 10, teacher_id, CURRENT_TIMESTAMP)
        RETURNING id INTO class_id;
    END IF;

    SELECT id INTO student_id FROM students WHERE email = 'student.demo@edusphere.test' LIMIT 1;
    IF student_id IS NULL THEN
        INSERT INTO students
            (admission_no, roll_number, first_name, last_name, email, password_hash,
             admission_date, course_id, status, created_at, updated_at)
        VALUES
            ('DEMO-S001', '01', 'Demo', 'Student', 'student.demo@edusphere.test',
             '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
             CURRENT_DATE, course_id, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        RETURNING id INTO student_id;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM class_students
         WHERE school_class_id = class_id AND student_id = student_id
    ) THEN
        INSERT INTO class_students (school_class_id, student_id, added_at)
        VALUES (class_id, student_id, CURRENT_TIMESTAMP);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM class_subjects
         WHERE school_class_id = class_id AND subject_code = 'DEMO-JAVA'
    ) THEN
        INSERT INTO class_subjects
            (school_class_id, subject_code, subject_name, credits, teacher_id,
             enrollment_mode, subject_id, created_at)
        VALUES
            (class_id, 'DEMO-JAVA', 'Java Programming', 4, teacher_id,
             'MANDATORY', subject1_id, CURRENT_TIMESTAMP);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM class_subjects
         WHERE school_class_id = class_id AND subject_code = 'DEMO-DBMS'
    ) THEN
        INSERT INTO class_subjects
            (school_class_id, subject_code, subject_name, credits, teacher_id,
             enrollment_mode, subject_id, created_at)
        VALUES
            (class_id, 'DEMO-DBMS', 'Database Management Systems', 4, teacher_id,
             'MANDATORY', subject2_id, CURRENT_TIMESTAMP);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM enrollments
         WHERE student_id = student_id AND subject_id = subject1_id
           AND academic_year = '2026-27' AND semester = 1
    ) THEN
        INSERT INTO enrollments
            (student_id, subject_id, academic_year, semester, enrollment_date, status, created_at)
        VALUES
            (student_id, subject1_id, '2026-27', 1, CURRENT_DATE, 'ACTIVE', CURRENT_TIMESTAMP);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM enrollments
         WHERE student_id = student_id AND subject_id = subject2_id
           AND academic_year = '2026-27' AND semester = 1
    ) THEN
        INSERT INTO enrollments
            (student_id, subject_id, academic_year, semester, enrollment_date, status, created_at)
        VALUES
            (student_id, subject2_id, '2026-27', 1, CURRENT_DATE, 'ACTIVE', CURRENT_TIMESTAMP);
    END IF;
END $$;