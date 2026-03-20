CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(50) NOT NULL,
    customer_name VARCHAR(100) NOT NULL,
    status VARCHAR(20),
    total_amount DECIMAL(19, 2) NOT NULL,
    ordered_at TIMESTAMP
);
