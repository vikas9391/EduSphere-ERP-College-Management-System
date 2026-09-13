CREATE TABLE class_holidays (
    id BIGSERIAL PRIMARY KEY,
    school_class_id BIGINT NOT NULL,
    holiday_date DATE NOT NULL,
    reason VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_class_holidays_school_class
        FOREIGN KEY (school_class_id) REFERENCES school_classes(id) ON DELETE CASCADE,
    CONSTRAINT uq_class_holidays_class_date UNIQUE (school_class_id, holiday_date)
);

CREATE INDEX idx_class_holidays_class_date ON class_holidays(school_class_id, holiday_date);
