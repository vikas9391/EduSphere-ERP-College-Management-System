CREATE TABLE marks (

    id BIGSERIAL PRIMARY KEY,

    exam_schedule_id BIGINT NOT NULL,

    student_id BIGINT NOT NULL,

    internal_marks INTEGER NOT NULL,

    external_marks INTEGER NOT NULL,

    total_marks INTEGER NOT NULL,

    grade VARCHAR(5) NOT NULL,

    grade_point DOUBLE PRECISION NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_marks_exam_schedule
        FOREIGN KEY(exam_schedule_id)
        REFERENCES exam_schedules(id),

    CONSTRAINT fk_marks_student
        FOREIGN KEY(student_id)
        REFERENCES students(id),

    CONSTRAINT uk_marks_schedule_student
        UNIQUE(exam_schedule_id, student_id)
);
