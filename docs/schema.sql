-- RayyanMart relational schema (MySQL 8 / H2 compatible).
-- Reference DDL documenting the Hibernate-managed model.
-- Hibernate normally creates the schema itself (ddl-auto=update); use this file
-- when pre-provisioning a database (then set DB_DDL_AUTO=validate).

CREATE TABLE IF NOT EXISTS users (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL DEFAULT 'CUSTOMER',
    created_at  DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email)
);

CREATE TABLE IF NOT EXISTS carts (
    id      BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_carts_user (user_id),
    CONSTRAINT fk_carts_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS products (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    name        VARCHAR(255)  NOT NULL,
    description VARCHAR(2000) NULL,
    price       DECIMAL(10,2) NOT NULL,
    quantity    INT           NOT NULL,
    image_url   VARCHAR(1000) NULL,
    category    VARCHAR(100)  NULL,
    available   BIT           NOT NULL DEFAULT 1,
    created_at  DATETIME      NOT NULL,
    seller_id   BIGINT        NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_products_seller FOREIGN KEY (seller_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS cart_items (
    id         BIGINT NOT NULL AUTO_INCREMENT,
    cart_id    BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity   INT    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_cart_items (cart_id, product_id),
    CONSTRAINT fk_cart_items_cart    FOREIGN KEY (cart_id)    REFERENCES carts (id),
    CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE TABLE IF NOT EXISTS orders (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    user_id      BIGINT        NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status       VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    created_at   DATETIME      NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS order_items (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    order_id     BIGINT        NOT NULL,
    product_id   BIGINT        NULL,
    product_name VARCHAR(255)  NOT NULL,
    quantity     INT           NOT NULL,
    price        DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order   FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE TABLE IF NOT EXISTS reviews (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    product_id BIGINT       NOT NULL,
    user_id    BIGINT       NOT NULL,
    rating     INT          NOT NULL,
    comment    VARCHAR(2000) NULL,
    created_at DATETIME     NOT NULL,
    updated_at DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_reviews (product_id, user_id),
    CONSTRAINT fk_reviews_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_reviews_user    FOREIGN KEY (user_id)    REFERENCES users (id)
);

-- Seed products (idempotent; matches src/main/resources/data.sql).
INSERT IGNORE INTO products (id, name, description, price, quantity, image_url, category, available, created_at) VALUES
(1, 'Wireless Mouse', 'Ergonomic 2.4GHz wireless mouse with silent clicks and 1600 DPI adjustable sensor.', 24.99, 100, 'https://placehold.co/400x300/4f9da6/fff?text=Wireless+Mouse', 'Electronics', true, NOW()),
(2, 'Java Programming Book', 'A comprehensive guide to modern Java development with practical examples.', 45.00, 50, 'https://placehold.co/400x300/6c4fa6/fff?text=Java+Book', 'Books', true, NOW()),
(3, 'Cotton T-Shirt', 'Soft premium 100% cotton t-shirt, available in multiple sizes.', 19.99, 200, 'https://placehold.co/400x300/a64f6c/fff?text=Cotton+T-Shirt', 'Clothing', true, NOW()),
(4, 'Leather Wallet', 'Genuine leather bi-fold wallet with RFID blocking protection.', 29.50, 75, 'https://placehold.co/400x300/a6844f/fff?text=Leather+Wallet', 'Accessories', true, NOW()),
(5, 'Ceramic Mug Set', 'Set of 4 dishwasher-safe ceramic mugs in assorted colours.', 15.75, 120, 'https://placehold.co/400x300/4fa67a/fff?text=Mug+Set', 'Home', true, NOW());