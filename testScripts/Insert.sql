INSERT INTO tenants (tenant_name) VALUES ('Tenant A'), ('Tenant B');

INSERT INTO roles (role_name) VALUES ('ADMIN'), ('USER');

INSERT INTO categories (category_name) VALUES ('Electronics'), ('Clothing');

INSERT INTO users (tenant_id, email, username)
VALUES (1,'admin@test.com','admin1'),
       (1,'user@test.com','user1');

INSERT INTO products (category_id, product_name, price)
VALUES (1,'Laptop',1200),
       (2,'Shirt',40);

INSERT INTO orders (user_id, tenant_id, order_date, status)
VALUES (1,1,CURDATE(),'NEW');

INSERT INTO order_items (order_id, product_id, quantity, price)
VALUES (1,1,1,1200);

INSERT INTO payments (order_id, user_id, amount, payment_status)
VALUES (1,1,1200,'PAID');