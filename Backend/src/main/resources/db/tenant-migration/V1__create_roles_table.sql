CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

INSERT INTO roles (name, description) VALUES
    ('COLLEGE_ADMIN', 'Full administrative access within this college'),
    ('FACULTY', 'Teaching staff access'),
    ('STUDENT', 'Student portal access'),
    ('PARENT', 'Parent portal access');