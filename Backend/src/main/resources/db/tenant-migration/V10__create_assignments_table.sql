CREATE TABLE assignments (

    id BIGSERIAL PRIMARY KEY,

    subject_id BIGINT NOT NULL,

    teacher_id BIGINT NOT NULL,

    title VARCHAR(200) NOT NULL,

    description TEXT,

    due_date DATE NOT NULL,

    max_marks INTEGER NOT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_assignment_subject
        FOREIGN KEY(subject_id)
        REFERENCES subjects(id),

    CONSTRAINT fk_assignment_teacher
        FOREIGN KEY(teacher_id)
        REFERENCES teachers(id)
);