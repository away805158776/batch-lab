-- Database schema for batch-lab

CREATE TABLE IF NOT EXISTS transaction_info (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(50) NOT NULL
);
