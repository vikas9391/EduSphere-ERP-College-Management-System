CREATE TABLE teachers (

    id BIGSERIAL PRIMARY KEY,

    employee_id VARCHAR(30) UNIQUE NOT NULL,

    first_name VARCHAR(100) NOT NULL,

    last_name VARCHAR(100),

    email VARCHAR(150) UNIQUE NOT NULL,

    phone VARCHAR(20),

    gender VARCHAR(20),

    qualification VARCHAR(100),

    specialization VARCHAR(100),

    experience INTEGER,

    joining_date DATE,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP

);