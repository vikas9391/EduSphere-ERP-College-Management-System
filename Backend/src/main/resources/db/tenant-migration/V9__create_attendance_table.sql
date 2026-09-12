-- Base attendance table. The class_enrollments table is created later (V17),
-- so its foreign key is finalized in V27 after the class-scoped academic tables exist.
CREATE TABLE attendance (
    id BIGSERIAL PRIMARY KEY,
    class_enrollment_id BIGINT NOT NULL,
    attendance_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    remarks VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);