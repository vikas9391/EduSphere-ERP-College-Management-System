CREATE TABLE enrollments (

    id BIGSERIAL PRIMARY KEY,

    student_id BIGINT NOT NULL,

    subject_id BIGINT NOT NULL,

    academic_year VARCHAR(20) NOT NULL,

    semester INTEGER NOT NULL,

    enrollment_date DATE NOT NULL,

    status VARCHAR(20) DEFAULT 'ACTIVE',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_enrollment_student
        FOREIGN KEY(student_id)
        REFERENCES students(id),

    CONSTRAINT fk_enrollment_subject
        FOREIGN KEY(subject_id)
        REFERENCES subjects(id),

    CONSTRAINT uk_student_subject
        UNIQUE(student_id, subject_id)
);