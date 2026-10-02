-- 在 DateTest 数据库中执行；已有表和数据不会被清空。
CREATE TABLE IF NOT EXISTS students (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    major VARCHAR(100),
    age INTEGER CHECK (age >= 0),
    enrollment_date DATE,
    graduated BOOLEAN NOT NULL DEFAULT FALSE,
    tuition NUMERIC(12, 2) CHECK (tuition >= 0)
);
CREATE TABLE IF NOT EXISTS colleges (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(32) NOT NULL UNIQUE
);
