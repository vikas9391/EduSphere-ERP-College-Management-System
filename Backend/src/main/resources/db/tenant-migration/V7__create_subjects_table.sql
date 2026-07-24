CREATE TABLE subjects (

    id BIGSERIAL PRIMARY KEY,

    subject_code VARCHAR(30) UNIQUE NOT NULL,

    subject_name VARCHAR(150) NOT NULL,

    credits INTEGER NOT NULL,

    semester INTEGER NOT NULL,

    course_id BIGINT NOT NULL,

    teacher_id BIGINT NOT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_subject_course
        FOREIGN KEY(course_id)
        REFERENCES courses(id),

    CONSTRAINT fk_subject_teacher
        FOREIGN KEY(teacher_id)
        REFERENCES teachers(id)
);