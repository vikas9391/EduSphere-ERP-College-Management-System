CREATE TABLE exams (

    id BIGSERIAL PRIMARY KEY,

    exam_name VARCHAR(150) NOT NULL,

    exam_type VARCHAR(30) NOT NULL,

    academic_year VARCHAR(20) NOT NULL,

    semester INTEGER NOT NULL,

    course_id BIGINT NOT NULL,

    start_date DATE NOT NULL,

    end_date DATE NOT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_exam_course
        FOREIGN KEY(course_id)
        REFERENCES courses(id)
);
