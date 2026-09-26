CREATE TABLE fee_structures (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT REFERENCES courses(id) ON DELETE SET NULL,
    academic_year VARCHAR(20) NOT NULL,
    semester INTEGER,
    name VARCHAR(150) NOT NULL,
    tuition_fee NUMERIC(12,2) NOT NULL DEFAULT 0,
    examination_fee NUMERIC(12,2) NOT NULL DEFAULT 0,
    library_fee NUMERIC(12,2) NOT NULL DEFAULT 0,
    other_fee NUMERIC(12,2) NOT NULL DEFAULT 0,
    due_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE student_fees (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    fee_structure_id BIGINT NOT NULL REFERENCES fee_structures(id) ON DELETE RESTRICT,
    discount NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_amount NUMERIC(12,2) NOT NULL,
    amount_paid NUMERIC(12,2) NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (student_id, fee_structure_id)
);

CREATE TABLE fee_payments (
    id BIGSERIAL PRIMARY KEY,
    student_fee_id BIGINT NOT NULL REFERENCES student_fees(id) ON DELETE RESTRICT,
    amount NUMERIC(12,2) NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    transaction_reference VARCHAR(120),
    receipt_number VARCHAR(60) NOT NULL UNIQUE,
    paid_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes VARCHAR(500)
);

CREATE INDEX idx_student_fees_student ON student_fees(student_id);
CREATE INDEX idx_fee_payments_student_fee ON fee_payments(student_fee_id);
