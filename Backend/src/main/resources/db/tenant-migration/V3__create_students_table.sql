CREATE TABLE students (

    id BIGSERIAL PRIMARY KEY,

    admission_no VARCHAR(30) UNIQUE NOT NULL,

    roll_number VARCHAR(30) UNIQUE,

    first_name VARCHAR(100) NOT NULL,

    last_name VARCHAR(100),

    email VARCHAR(150) UNIQUE NOT NULL,

    password_hash VARCHAR(255) NOT NULL,

    phone VARCHAR(20),

    gender VARCHAR(20),

    date_of_birth DATE,

    admission_date DATE,

    address TEXT,

    city VARCHAR(100),

    state VARCHAR(100),

    country VARCHAR(100),

    pincode VARCHAR(20),

    father_name VARCHAR(100),

    mother_name VARCHAR(100),

    parent_phone VARCHAR(20),

    parent_email VARCHAR(150),

    blood_group VARCHAR(10),

    category VARCHAR(50),

    nationality VARCHAR(100),

    aadhaar_number VARCHAR(20),

    photo_url TEXT,

    status VARCHAR(20) DEFAULT 'ACTIVE',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);