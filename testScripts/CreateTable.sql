SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS audit_logs, notifications, payments, shipments, invoices,
    order_items, orders, products, categories, inventory,
    users, roles, user_roles, tenants;

SET FOREIGN_KEY_CHECKS = 1;

-- =============================
-- TENANT DOMAIN
-- =============================

CREATE TABLE tenants (
                         tenant_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                         tenant_name VARCHAR(100),
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================
-- USER DOMAIN
-- =============================

CREATE TABLE users (
                       user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                       tenant_id BIGINT,
                       email VARCHAR(150),
                       username VARCHAR(100),
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id)
);

CREATE TABLE roles (
                       role_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                       role_name VARCHAR(100)
);

CREATE TABLE user_roles (
                            user_id BIGINT,
                            role_id BIGINT,
                            PRIMARY KEY(user_id, role_id),
                            FOREIGN KEY (user_id) REFERENCES users(user_id),
                            FOREIGN KEY (role_id) REFERENCES roles(role_id)
);

-- =============================
-- PRODUCT DOMAIN
-- =============================

CREATE TABLE categories (
                            category_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                            category_name VARCHAR(100)
);

CREATE TABLE products (
                          product_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                          category_id BIGINT,
                          product_name VARCHAR(150),
                          price DECIMAL(10,2),
                          FOREIGN KEY (category_id) REFERENCES categories(category_id)
);

CREATE TABLE inventory (
                           inventory_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                           product_id BIGINT,
                           quantity INT,
                           warehouse_code VARCHAR(50),
                           FOREIGN KEY (product_id) REFERENCES products(product_id)
);

-- =============================
-- ORDER DOMAIN
-- =============================

CREATE TABLE orders (
                        order_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        user_id BIGINT,
                        tenant_id BIGINT,
                        order_date DATE,
                        status VARCHAR(50),
                        FOREIGN KEY (user_id) REFERENCES users(user_id),
                        FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id)
);

CREATE TABLE order_items (
                             order_item_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                             order_id BIGINT,
                             product_id BIGINT,
                             quantity INT,
                             price DECIMAL(10,2),
                             FOREIGN KEY (order_id) REFERENCES orders(order_id),
                             FOREIGN KEY (product_id) REFERENCES products(product_id)
);

-- =============================
-- PAYMENT DOMAIN
-- =============================

CREATE TABLE payments (
                          payment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                          order_id BIGINT,
                          user_id BIGINT,
                          amount DECIMAL(10,2),
                          payment_status VARCHAR(50),
                          FOREIGN KEY (order_id) REFERENCES orders(order_id),
                          FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE invoices (
                          invoice_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                          order_id BIGINT,
                          total_amount DECIMAL(10,2),
                          generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          FOREIGN KEY (order_id) REFERENCES orders(order_id)
);

-- =============================
-- SHIPPING DOMAIN
-- =============================

CREATE TABLE shipments (
                           shipment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                           order_id BIGINT,
                           tracking_number VARCHAR(100),
                           shipped_date DATE,
                           FOREIGN KEY (order_id) REFERENCES orders(order_id)
);

-- =============================
-- AUDIT DOMAIN
-- =============================

CREATE TABLE audit_logs (
                            audit_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                            user_id BIGINT,
                            entity_name VARCHAR(100),
                            entity_id BIGINT,
                            action_type VARCHAR(50),
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- =============================
-- NOTIFICATION DOMAIN
-- =============================

CREATE TABLE notifications (
                               notification_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                               user_id BIGINT,
                               message TEXT,
                               sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               FOREIGN KEY (user_id) REFERENCES users(user_id)
);