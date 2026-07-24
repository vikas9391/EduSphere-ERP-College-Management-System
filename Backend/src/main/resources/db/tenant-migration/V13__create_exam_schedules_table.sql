CREATE TABLE exam_schedules (

    id BIGSERIAL PRIMARY KEY,

    exam_id BIGINT NOT NULL,

    subject_id BIGINT NOT NULL,

    invigilator_id BIGINT,

    exam_date DATE NOT NULL,

    start_time TIME NOT NULL,

    end_time TIME NOT NULL,

    room VARCHAR(50),

    max_marks INTEGER NOT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_schedule_exam
        FOREIGN KEY(exam_id)
        REFERENCES exams(id),

    CONSTRAINT fk_schedule_subject
        FOREIGN KEY(subject_id)
        REFERENCES subjects(id),

    CONSTRAINT fk_schedule_invigilator
        FOREIGN KEY(invigilator_id)
        REFERENCES teachers(id),

    CONSTRAINT uk_exam_subject
        UNIQUE(exam_id, subject_id)
);
