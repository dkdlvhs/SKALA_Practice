INSERT INTO customers (customer_id, customer_password, customer_point, role) VALUES
    ('user', 'password', 10000.0, 'USER'),
    ('admin', 'password', 1000000000.0, 'ADMIN');

INSERT INTO products (id, product_name, product_price) VALUES
    (1, '아메리카노', 4500.0),
    (2, '카페라떼', 5000.0),
    (3, '바닐라라떼', 5500.0),
    (4, '카라멜마키아토', 6000.0),
    (5, '핫초코', 4800.0),
    (6, '녹차라떼', 5200.0),
    (7, '딸기스무디', 5900.0),
    (8, '망고주스', 6200.0),
    (9, '샌드위치', 7500.0),
    (10, '크로와상', 4000.0);

ALTER TABLE products ALTER COLUMN id RESTART WITH 11;
