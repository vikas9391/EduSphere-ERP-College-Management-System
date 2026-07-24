CREATE TABLE assignment_submissions (

    id BIGSERIAL PRIMARY KEY,

    assignment_id BIGINT NOT NULL,

    student_id BIGINT NOT NULL,

    submission_url TEXT,

    submitted_at TIMESTAMP,

    marks INTEGER,

    feedback TEXT,

    status VARCHAR(30),

    CONSTRAINT fk_submission_assignment
        FOREIGN KEY(assignment_id)
        REFERENCES assignments(id),

    CONSTRAINT fk_submission_student
        FOREIGN KEY(student_id)
        REFERENCES students(id),

    CONSTRAINT uk_assignment_student
        UNIQUE(assignment_id, student_id)
);