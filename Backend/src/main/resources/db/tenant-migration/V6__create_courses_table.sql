CREATE TABLE courses (

    id BIGSERIAL PRIMARY KEY,

    course_code VARCHAR(30) UNIQUE NOT NULL,

    course_name VARCHAR(150) NOT NULL,

    duration INTEGER,

    description TEXT,

    department_id BIGINT NOT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_course_department
        FOREIGN KEY (department_id)
        REFERENCES departments(id)
        ON DELETE CASCADE

);