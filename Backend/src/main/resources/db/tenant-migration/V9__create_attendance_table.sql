CREATE TABLE attendance (

    id BIGSERIAL PRIMARY KEY,

    enrollment_id BIGINT NOT NULL,

    attendance_date DATE NOT NULL,

    status VARCHAR(20) NOT NULL,

    remarks VARCHAR(255),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_attendance_enrollment
        FOREIGN KEY (enrollment_id)
        REFERENCES enrollments(id),

    CONSTRAINT uk_attendance
        UNIQUE(enrollment_id, attendance_date)
);