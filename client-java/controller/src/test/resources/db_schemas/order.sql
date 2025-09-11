CREATE TABLE users (
                       user_id BIGINT NOT NULL AUTO_INCREMENT,
                       username VARCHAR(50) NOT NULL UNIQUE,
                       email VARCHAR(100) NOT NULL UNIQUE,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       PRIMARY KEY (user_id));

CREATE TABLE user_addresses (
                                address_id BIGINT NOT NULL AUTO_INCREMENT,
                                user_id BIGINT NOT NULL,
                                address_line VARCHAR(255) NOT NULL,
                                city VARCHAR(100),
                                postal_code VARCHAR(20),
                                country VARCHAR(50) DEFAULT 'China',
                                is_default BOOLEAN DEFAULT FALSE,
                                PRIMARY KEY (address_id),
                                FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE categories (
                            category_id BIGINT NOT NULL AUTO_INCREMENT,
                            name VARCHAR(100) NOT NULL UNIQUE,
                            PRIMARY KEY (category_id),
                            description TEXT
);

CREATE TABLE products (
                          product_id BIGINT NOT NULL AUTO_INCREMENT,
                          name VARCHAR(200) NOT NULL,
                          description TEXT,
                          price DECIMAL(10, 2) NOT NULL,
                          stock INT DEFAULT 0,
                          category_id BIGINT,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          PRIMARY KEY (product_id),
                          FOREIGN KEY (category_id) REFERENCES categories(category_id) ON DELETE SET NULL
);

CREATE TABLE orders (
                        order_id BIGINT NOT NULL AUTO_INCREMENT,
                        user_id BIGINT NOT NULL,
                        total_amount DECIMAL(12, 2) NOT NULL,
                        status ENUM('pending', 'paid', 'shipped', 'delivered', 'cancelled') DEFAULT 'pending',
                        order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        PRIMARY KEY (order_id),
                        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE order_items (
                             item_id BIGINT NOT NULL AUTO_INCREMENT,
                             order_id BIGINT NOT NULL,
                             product_id BIGINT NOT NULL,
                             quantity INT NOT NULL CHECK (quantity > 0),
                             unit_price DECIMAL(10, 2) NOT NULL,
                             PRIMARY KEY (item_id),
                             FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
                             FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE
);

CREATE TABLE payments (
                          payment_id BIGINT NOT NULL AUTO_INCREMENT,
                          order_id BIGINT NOT NULL UNIQUE,
                          amount DECIMAL(12, 2) NOT NULL,
                          payment_method VARCHAR(50) NOT NULL,
                          payment_status ENUM('pending', 'completed', 'failed') DEFAULT 'pending',
                          transaction_id VARCHAR(100),
                          paid_at TIMESTAMP NULL,
                          PRIMARY KEY (payment_id),
                          FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE
);

CREATE TABLE reviews (
                         review_id BIGINT NOT NULL AUTO_INCREMENT,
                         user_id BIGINT NOT NULL,
                         product_id BIGINT NOT NULL,
                         rating TINYINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
                         comment TEXT,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         PRIMARY KEY (review_id),
                         FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                         FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE
);

CREATE TABLE coupons (
                         coupon_id BIGINT NOT NULL AUTO_INCREMENT,
                         code VARCHAR(50) NOT NULL UNIQUE,
                         discount_percent DECIMAL(5, 2) CHECK (discount_percent BETWEEN 0 AND 100),
                         valid_from DATE,
                         valid_until DATE,
                         is_active BOOLEAN DEFAULT TRUE,
                         PRIMARY KEY (coupon_id)
);

CREATE TABLE user_coupons (
                              user_coupon_id BIGINT NOT NULL AUTO_INCREMENT,
                              user_id BIGINT NOT NULL,
                              coupon_id BIGINT NOT NULL,
                              used BOOLEAN DEFAULT FALSE,
                              used_at TIMESTAMP NULL,
                              FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                              FOREIGN KEY (coupon_id) REFERENCES coupons(coupon_id) ON DELETE CASCADE,
                              UNIQUE (user_id, coupon_id),
                              PRIMARY KEY (user_coupon_id)
);