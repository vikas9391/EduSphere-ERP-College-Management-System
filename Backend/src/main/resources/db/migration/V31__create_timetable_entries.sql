CREATE TABLE timetable_entries (
    id BIGSERIAL PRIMARY KEY,
    class_subject_id BIGINT NOT NULL,
    day_of_week VARCHAR(16) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    room VARCHAR(120),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_timetable_class_subject
        FOREIGN KEY (class_subject_id) REFERENCES class_subjects(id) ON DELETE CASCADE,
    CONSTRAINT chk_timetable_time_order CHECK (end_time > start_time),
    CONSTRAINT uq_timetable_class_subject_slot UNIQUE (class_subject_id, day_of_week, start_time)
);

CREATE INDEX idx_timetable_class_subject ON timetable_entries(class_subject_id);
CREATE INDEX idx_timetable_day_time ON timetable_entries(day_of_week, start_time);
