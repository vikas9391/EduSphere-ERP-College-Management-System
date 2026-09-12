CREATE TABLE attendance (

    id BIGSERIAL PRIMARY KEY,

    class_enrollment_id BIGINT NOT NULL,

    attendance_date DATE NOT NULL,

    status VARCHAR(20) NOT NULL,

    remarks VARCHAR(255),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_attendance_class_enrollment
        FOREIGN KEY (class_enrollment_id)
        REFERENCES class_enrollments(id)
        ON DELETE CASCADE,

    CONSTRAINT uk_attendance
        UNIQUE(class_enrollment_id, attendance_date)
);