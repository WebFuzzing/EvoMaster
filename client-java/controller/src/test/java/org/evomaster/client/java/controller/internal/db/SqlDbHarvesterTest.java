package org.evomaster.client.java.controller.internal.db;

import org.evomaster.client.java.controller.DatabaseTestTemplate;
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;
import org.evomaster.client.java.controller.internal.SutController;
import org.evomaster.client.java.controller.internal.db.sql.mysql.DatabaseFakeMySQLSutController;
import org.evomaster.client.java.controller.internal.db.sql.mysql.DatabaseMySQLTestInit;
import org.evomaster.client.java.sql.DbInfoExtractor;
import org.evomaster.client.java.sql.SqlScriptRunner;
import org.evomaster.client.java.sql.internal.SqlDbHarvester;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.List;

public class SqlDbHarvesterTest extends DatabaseMySQLTestInit implements DatabaseTestTemplate {



    //@Test
    public void testInitScript() throws Exception{
        String initTablesScripts =
                "CREATE TABLE users (\n" +
                        "    user_id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,\n" +
                        "    username VARCHAR(50) NOT NULL UNIQUE,\n" +
                        "    email VARCHAR(100) NOT NULL UNIQUE,\n" +
                        "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n" +
                        ");\n" +
                        "\n" +
                        "CREATE TABLE user_addresses (\n" +
                        "    address_id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,\n" +
                        "    user_id BIGINT NOT NULL,\n" +
                        "    address_line VARCHAR(255) NOT NULL,\n" +
                        "    city VARCHAR(100),\n" +
                        "    postal_code VARCHAR(20),\n" +
                        "    country VARCHAR(50) DEFAULT 'China',\n" +
                        "    is_default BOOLEAN DEFAULT FALSE,\n" +
                        "    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE\n" +
                        ");\n" +
                        "\n" +
                        "CREATE TABLE categories (\n" +
                        "    category_id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,\n" +
                        "    name VARCHAR(100) NOT NULL UNIQUE,\n" +
                        "    description TEXT\n" +
                        ");\n" +
                        "\n" +
                        "CREATE TABLE products (\n" +
                        "    product_id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,\n" +
                        "    name VARCHAR(200) NOT NULL,\n" +
                        "    description TEXT,\n" +
                        "    price DECIMAL(10, 2) NOT NULL,\n" +
                        "    stock INT DEFAULT 0,\n" +
                        "    category_id BIGINT,\n" +
                        "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
                        "    FOREIGN KEY (category_id) REFERENCES categories(category_id) ON DELETE SET NULL\n" +
                        ");\n" +
                        "\n" +
                        "CREATE TABLE orders (\n" +
                        "    order_id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,\n" +
                        "    user_id BIGINT NOT NULL,\n" +
                        "    total_amount DECIMAL(12, 2) NOT NULL,\n" +
                        "    status ENUM('pending', 'paid', 'shipped', 'delivered', 'cancelled') DEFAULT 'pending',\n" +
                        "    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
                        "    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE\n" +
                        ");\n" +
                        "\n" +
                        "CREATE TABLE order_items (\n" +
                        "    item_id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,\n" +
                        "    order_id BIGINT NOT NULL,\n" +
                        "    product_id BIGINT NOT NULL,\n" +
                        "    quantity INT NOT NULL CHECK (quantity > 0),\n" +
                        "    unit_price DECIMAL(10, 2) NOT NULL,\n" +
                        "    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,\n" +
                        "    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE\n" +
                        ");\n" +
                        "\n" +
                        "CREATE TABLE payments (\n" +
                        "    payment_id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,\n" +
                        "    order_id BIGINT NOT NULL UNIQUE,\n" +
                        "    amount DECIMAL(12, 2) NOT NULL,\n" +
                        "    payment_method VARCHAR(50) NOT NULL,\n" +
                        "    payment_status ENUM('pending', 'completed', 'failed') DEFAULT 'pending',\n" +
                        "    transaction_id VARCHAR(100),\n" +
                        "    paid_at TIMESTAMP NULL,\n" +
                        "    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE\n" +
                        ");\n" +
                        "\n" +
                        "CREATE TABLE reviews (\n" +
                        "    review_id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,\n" +
                        "    user_id BIGINT NOT NULL,\n" +
                        "    product_id BIGINT NOT NULL,\n" +
                        "    rating TINYINT NOT NULL CHECK (rating BETWEEN 1 AND 5),\n" +
                        "    comment TEXT,\n" +
                        "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
                        "    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,\n" +
                        "    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE\n" +
                        ");\n" +
                        "\n" +
                        "CREATE TABLE coupons (\n" +
                        "    coupon_id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,\n" +
                        "    code VARCHAR(50) NOT NULL UNIQUE,\n" +
                        "    discount_percent DECIMAL(5, 2) CHECK (discount_percent BETWEEN 0 AND 100),\n" +
                        "    valid_from DATE,\n" +
                        "    valid_until DATE,\n" +
                        "    is_active BOOLEAN DEFAULT TRUE\n" +
                        ");\n" +
                        "\n" +
                        "CREATE TABLE user_coupons (\n" +
                        "    user_coupon_id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,\n" +
                        "    user_id BIGINT NOT NULL,\n" +
                        "    coupon_id BIGINT NOT NULL,\n" +
                        "    used BOOLEAN DEFAULT FALSE,\n" +
                        "    used_at TIMESTAMP NULL,\n" +
                        "    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,\n" +
                        "    FOREIGN KEY (coupon_id) REFERENCES coupons(coupon_id) ON DELETE CASCADE,\n" +
                        "    UNIQUE (user_id, coupon_id)\n" +
                        ");";

        SqlScriptRunner.execCommand(getConnection(),initTablesScripts);


        String initRecordsScripts =
                "INSERT INTO users (username, email) VALUES\n" +
                        "('user01', 'user01@example.com'),\n" +
                        "('user02', 'user02@example.com'),\n" +
                        "('user03', 'user03@example.com'),\n" +
                        "('user04', 'user04@example.com'),\n" +
                        "('user05', 'user05@example.com'),\n" +
                        "('user06', 'user06@example.com'),\n" +
                        "('user07', 'user07@example.com'),\n" +
                        "('user08', 'user08@example.com'),\n" +
                        "('user09', 'user09@example.com'),\n" +
                        "('user10', 'user10@example.com'),\n" +
                        "('user11', 'user11@example.com'),\n" +
                        "('user12', 'user12@example.com'),\n" +
                        "('user13', 'user13@example.com'),\n" +
                        "('user14', 'user14@example.com'),\n" +
                        "('user15', 'user15@example.com'),\n" +
                        "('user16', 'user16@example.com'),\n" +
                        "('user17', 'user17@example.com'),\n" +
                        "('user18', 'user18@example.com'),\n" +
                        "('user19', 'user19@example.com'),\n" +
                        "('user20', 'user20@example.com');\n" +
                        "\n" +
                        "INSERT INTO categories (name, description) VALUES\n" +
                        "('Electronics', 'Smartphones, laptops, accessories'),\n" +
                        "('Books', 'Fiction, non-fiction, educational'),\n" +
                        "('Clothing', 'Men, women, and children apparel'),\n" +
                        "('Home & Kitchen', 'Furniture, utensils, decor'),\n" +
                        "('Sports', 'Fitness equipment, sportswear');\n" +
                        "\n" +
                        "INSERT INTO products (name, description, price, stock, category_id) VALUES\n" +
                        "('iPhone 15', 'Latest Apple smartphone', 999.99, 50, 1),\n" +
                        "('Samsung Galaxy S24', 'Android flagship', 899.99, 40, 1),\n" +
                        "('MacBook Pro 16\"', 'Powerful laptop for pros', 2499.99, 20, 1),\n" +
                        "('Dell XPS 13', 'Ultra portable business laptop', 1199.99, 25, 1),\n" +
                        "('iPad Air', 'Lightweight tablet', 599.99, 60, 1),\n" +
                        "('Harry Potter Set', 'Complete 7-book collection', 89.99, 100, 2),\n" +
                        "('The Alchemist', 'Paulo Coelho bestseller', 12.99, 200, 2),\n" +
                        "('Atomic Habits', 'Self-improvement guide', 14.99, 150, 2),\n" +
                        "('Dune', 'Sci-fi classic by Frank Herbert', 16.99, 120, 2),\n" +
                        "('T-Shirt Cotton White', 'Comfortable everyday wear', 19.99, 300, 3),\n" +
                        "('Jeans Slim Fit Blue', 'Classic denim jeans', 49.99, 150, 3),\n" +
                        "('Running Shoes Nike', 'Lightweight for jogging', 89.99, 80, 3),\n" +
                        "('Yoga Mat', 'Non-slip exercise mat', 29.99, 120, 5),\n" +
                        "('Dumbbell Set 10kg', 'Adjustable weight set', 199.99, 40, 5),\n" +
                        "('Coffee Maker', 'Automatic drip machine', 79.99, 70, 4),\n" +
                        "('Air Fryer', 'Healthy oil-free frying', 129.99, 50, 4),\n" +
                        "('Blender 1000W', 'Powerful kitchen blender', 59.99, 90, 4),\n" +
                        "('Desk Lamp LED', 'Adjustable brightness', 35.99, 110, 4),\n" +
                        "('Water Bottle 1L', 'Stainless steel insulated', 25.99, 200, 5),\n" +
                        "('Backpack Waterproof', 'For hiking and travel', 69.99, 80, 5);\n" +
                        "\n" +
                        "INSERT INTO user_addresses (user_id, address_line, city, postal_code, country, is_default) VALUES\n" +
                        "(1, '123 Main St', 'Beijing', '100001', 'China', TRUE),\n" +
                        "(2, '456 Oak Ave', 'Shanghai', '200001', 'China', TRUE),\n" +
                        "(3, '789 Pine Rd', 'Guangzhou', '510001', 'China', FALSE),\n" +
                        "(4, '321 Elm Blvd', 'Shenzhen', '518001', 'China', TRUE),\n" +
                        "(5, '654 Cedar Ln', 'Hangzhou', '310001', 'China', TRUE),\n" +
                        "(6, '987 Maple Dr', 'Chengdu', '610001', 'China', FALSE),\n" +
                        "(7, '147 Birch St', 'Chongqing', '400001', 'China', TRUE),\n" +
                        "(8, '258 Spruce Ave', 'Nanjing', '210001', 'China', TRUE),\n" +
                        "(9, '369 Willow Rd', 'Wuhan', '430001', 'China', FALSE),\n" +
                        "(10, '159 Aspen Blvd', 'Xi’an', '710001', 'China', TRUE),\n" +
                        "(11, '357 Redwood Ln', 'Tianjin', '300001', 'China', TRUE),\n" +
                        "(12, '753 Sequoia Dr', 'Suzhou', '215001', 'China', FALSE),\n" +
                        "(13, '951 Fir St', 'Dalian', '116001', 'China', TRUE),\n" +
                        "(14, '852 Cypress Ave', 'Qingdao', '266001', 'China', TRUE),\n" +
                        "(15, '741 Palm Rd', 'Xiamen', '361001', 'China', FALSE),\n" +
                        "(16, '630 Olive Blvd', 'Changsha', '410001', 'China', TRUE),\n" +
                        "(17, '520 Cherry Ln', 'Zhengzhou', '450001', 'China', TRUE),\n" +
                        "(18, '410 Peach Dr', 'Harbin', '150001', 'China', FALSE),\n" +
                        "(19, '300 Plum St', 'Kunming', '650001', 'China', TRUE),\n" +
                        "(20, '200 Orange Ave', 'Fuzhou', '350001', 'China', TRUE);\n" +
                        "\n" +
                        "INSERT INTO orders (user_id, total_amount, status, order_date) VALUES\n" +
                        "(1, 1299.98, 'delivered', '2025-03-01 10:30:00'),\n" +
                        "(2, 89.99, 'shipped', '2025-03-02 14:15:00'),\n" +
                        "(3, 2499.99, 'paid', '2025-03-03 09:00:00'),\n" +
                        "(4, 109.98, 'pending', '2025-03-04 16:45:00'),\n" +
                        "(5, 199.99, 'delivered', '2025-02-28 11:20:00'),\n" +
                        "(6, 359.97, 'shipped', '2025-03-05 13:10:00'),\n" +
                        "(7, 79.99, 'delivered', '2025-03-01 17:50:00'),\n" +
                        "(8, 159.98, 'paid', '2025-03-06 08:30:00'),\n" +
                        "(9, 89.99, 'pending', '2025-03-07 12:00:00'),\n" +
                        "(10, 299.98, 'shipped', '2025-03-08 15:30:00'),\n" +
                        "(11, 59.99, 'delivered', '2025-03-02 10:00:00'),\n" +
                        "(12, 189.98, 'paid', '2025-03-09 14:20:00'),\n" +
                        "(13, 69.99, 'pending', '2025-03-10 09:45:00'),\n" +
                        "(14, 109.98, 'shipped', '2025-03-11 16:10:00'),\n" +
                        "(15, 2499.99, 'delivered', '2025-02-27 11:00:00'),\n" +
                        "(16, 149.98, 'paid', '2025-03-12 13:30:00'),\n" +
                        "(17, 129.99, 'pending', '2025-03-13 08:15:00'),\n" +
                        "(18, 39.98, 'shipped', '2025-03-14 17:00:00'),\n" +
                        "(19, 199.99, 'delivered', '2025-03-03 12:30:00'),\n" +
                        "(20, 89.99, 'paid', '2025-03-15 10:45:00');\n" +
                        "\n" +
                        "INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES\n" +
                        "(1, 1, 1, 999.99),  -- iPhone\n" +
                        "(1, 6, 3, 89.99),   -- Books\n" +
                        "(2, 6, 1, 89.99),\n" +
                        "(3, 3, 1, 2499.99),\n" +
                        "(4, 10, 2, 19.99),  -- T-Shirts\n" +
                        "(4, 11, 1, 49.99),  -- Jeans\n" +
                        "(5, 13, 1, 199.99), -- Dumbbell\n" +
                        "(6, 16, 1, 129.99), -- Air Fryer\n" +
                        "(6, 17, 1, 59.99),  -- Blender\n" +
                        "(6, 18, 2, 35.99),  -- Lamps\n" +
                        "(7, 16, 1, 79.99),\n" +
                        "(8, 12, 1, 89.99),  -- Shoes\n" +
                        "(8, 14, 1, 29.99),  -- Yoga Mat\n" +
                        "(9, 12, 1, 89.99),\n" +
                        "(10, 2, 1, 899.99), -- Galaxy\n" +
                        "(10, 19, 1, 25.99), -- Bottle\n" +
                        "(11, 17, 1, 59.99),\n" +
                        "(12, 10, 5, 19.99), -- 5 T-Shirts\n" +
                        "(12, 11, 2, 49.99), -- 2 Jeans\n" +
                        "(13, 20, 1, 69.99), -- Backpack\n" +
                        "(14, 7, 2, 12.99),  -- 2x The Alchemist\n" +
                        "(14, 8, 2, 14.99),  -- 2x Atomic Habits\n" +
                        "(15, 3, 1, 2499.99),\n" +
                        "(16, 8, 3, 14.99),  -- 3x Atomic Habits\n" +
                        "(16, 9, 3, 16.99),  -- 3x Dune\n" +
                        "(17, 16, 1, 129.99),\n" +
                        "(18, 18, 1, 35.99), -- Lamp\n" +
                        "(18, 19, 1, 25.99), -- Bottle\n" +
                        "(19, 13, 1, 199.99),\n" +
                        "(20, 12, 1, 89.99);\n" +
                        "\n" +
                        "INSERT INTO payments (order_id, amount, payment_method, payment_status, transaction_id, paid_at) VALUES\n" +
                        "(1, 1299.98, 'Credit Card', 'completed', 'txn_001', '2025-03-01 10:35:00'),\n" +
                        "(2, 89.99, 'Alipay', 'completed', 'txn_002', '2025-03-02 14:20:00'),\n" +
                        "(3, 2499.99, 'WeChat Pay', 'completed', 'txn_003', '2025-03-03 09:05:00'),\n" +
                        "(4, 109.98, 'Credit Card', 'pending', NULL, NULL),\n" +
                        "(5, 199.99, 'Alipay', 'completed', 'txn_005', '2025-02-28 11:25:00'),\n" +
                        "(6, 359.97, 'WeChat Pay', 'completed', 'txn_006', '2025-03-05 13:15:00'),\n" +
                        "(7, 79.99, 'Credit Card', 'completed', 'txn_007', '2025-03-01 17:55:00'),\n" +
                        "(8, 159.98, 'Alipay', 'completed', 'txn_008', '2025-03-06 08:35:00'),\n" +
                        "(9, 89.99, 'WeChat Pay', 'pending', NULL, NULL),\n" +
                        "(10, 299.98, 'Credit Card', 'completed', 'txn_010', '2025-03-08 15:35:00'),\n" +
                        "(11, 59.99, 'Alipay', 'completed', 'txn_011', '2025-03-02 10:05:00'),\n" +
                        "(12, 189.98, 'WeChat Pay', 'completed', 'txn_012', '2025-03-09 14:25:00'),\n" +
                        "(13, 69.99, 'Credit Card', 'pending', NULL, NULL),\n" +
                        "(14, 109.98, 'Alipay', 'completed', 'txn_014', '2025-03-11 16:15:00'),\n" +
                        "(15, 2499.99, 'Credit Card', 'completed', 'txn_015', '2025-02-27 11:05:00'),\n" +
                        "(16, 149.98, 'WeChat Pay', 'completed', 'txn_016', '2025-03-12 13:35:00'),\n" +
                        "(17, 129.99, 'Alipay', 'pending', NULL, NULL),\n" +
                        "(18, 39.98, 'Credit Card', 'completed', 'txn_018', '2025-03-14 17:05:00'),\n" +
                        "(19, 199.99, 'WeChat Pay', 'completed', 'txn_019', '2025-03-03 12:35:00'),\n" +
                        "(20, 89.99, 'Alipay', 'completed', 'txn_020', '2025-03-15 10:50:00');\n" +
                        "\n" +
                        "INSERT INTO reviews (user_id, product_id, rating, comment) VALUES\n" +
                        "(1, 1, 5, 'Amazing phone, worth every penny!'),\n" +
                        "(2, 6, 4, 'Great book set for Harry Potter fans.'),\n" +
                        "(3, 3, 5, 'Best laptop I have ever used.'),\n" +
                        "(4, 10, 3, 'Comfortable but runs a bit small.'),\n" +
                        "(5, 13, 4, 'Good quality dumbbells, solid build.'),\n" +
                        "(6, 16, 5, 'Love my air fryer, cooks perfectly.'),\n" +
                        "(7, 16, 4, 'Easy to use, cleans up nicely.'),\n" +
                        "(8, 12, 5, 'Perfect for my morning runs.'),\n" +
                        "(9, 12, 4, 'Good grip and cushioning.'),\n" +
                        "(10, 2, 4, 'Great Android phone, battery life is good.'),\n" +
                        "(11, 17, 3, 'Blender is okay, a bit noisy.'),\n" +
                        "(12, 10, 5, 'Bought 5 for my team, all love them!'),\n" +
                        "(13, 20, 4, 'Spacious and waterproof, great for travel.'),\n" +
                        "(14, 7, 5, 'Life-changing book, highly recommend.'),\n" +
                        "(15, 3, 5, 'Worth the investment for professionals.'),\n" +
                        "(16, 8, 5, 'Helped me build better habits.'),\n" +
                        "(17, 16, 4, 'Heats up fast, easy controls.'),\n" +
                        "(18, 18, 3, 'Lamp is functional but design is plain.'),\n" +
                        "(19, 13, 5, 'Perfect for home workouts.'),\n" +
                        "(20, 12, 4, 'Comfortable for long walks too.');\n" +
                        "\n" +
                        "INSERT INTO coupons (code, discount_percent, valid_from, valid_until, is_active) VALUES\n" +
                        "('WELCOME10', 10.00, '2025-01-01', '2025-12-31', TRUE),\n" +
                        "('SPRING20', 20.00, '2025-03-01', '2025-04-30', TRUE),\n" +
                        "('SUMMER25', 25.00, '2025-06-01', '2025-08-31', FALSE),\n" +
                        "('HOLIDAY30', 30.00, '2025-12-01', '2025-12-31', FALSE),\n" +
                        "('FLASH5', 5.00, '2025-03-15', '2025-03-16', TRUE),\n" +
                        "('VIP15', 15.00, '2025-01-01', '2025-12-31', TRUE),\n" +
                        "('NEWUSER10', 10.00, '2025-01-01', '2025-12-31', TRUE),\n" +
                        "('ELECTRO15', 15.00, '2025-03-01', '2025-03-31', TRUE),\n" +
                        "('BOOKS20', 20.00, '2025-03-01', '2025-03-31', TRUE),\n" +
                        "('FASHION10', 10.00, '2025-03-01', '2025-04-15', TRUE),\n" +
                        "('FITNESS25', 25.00, '2025-05-01', '2025-06-30', FALSE),\n" +
                        "('KITCHEN15', 15.00, '2025-03-01', '2025-04-30', TRUE),\n" +
                        "('BACKTOSCHOOL', 12.00, '2025-08-01', '2025-09-15', FALSE),\n" +
                        "('LOYALTY5', 5.00, '2025-01-01', '2025-12-31', TRUE),\n" +
                        "('FREESHIP', 0.00, '2025-03-01', '2025-03-31', TRUE); -- Free shipping coupon\n" +
                        "\n" +
                        "INSERT INTO user_coupons (user_id, coupon_id, used, used_at) VALUES\n" +
                        "(1, 1, TRUE, '2025-03-01 10:30:00'),\n" +
                        "(2, 1, TRUE, '2025-03-02 14:15:00'),\n" +
                        "(3, 2, FALSE, NULL),\n" +
                        "(4, 3, FALSE, NULL),\n" +
                        "(5, 4, FALSE, NULL),\n" +
                        "(6, 5, TRUE, '2025-03-05 13:10:00'),\n" +
                        "(7, 6, TRUE, '2025-03-01 17:50:00'),\n" +
                        "(8, 7, TRUE, '2025-03-06 08:30:00'),\n" +
                        "(9, 8, FALSE, NULL),\n" +
                        "(10, 9, TRUE, '2025-03-08 15:30:00'),\n" +
                        "(11, 10, TRUE, '2025-03-02 10:00:00'),\n" +
                        "(12, 11, FALSE, NULL),\n" +
                        "(13, 12, FALSE, NULL),\n" +
                        "(14, 13, FALSE, NULL),\n" +
                        "(15, 14, TRUE, '2025-02-27 11:00:00'),\n" +
                        "(16, 15, TRUE, '2025-03-12 13:30:00'),\n" +
                        "(17, 1, TRUE, '2025-03-13 08:15:00'),\n" +
                        "(18, 2, FALSE, NULL),\n" +
                        "(19, 3, FALSE, NULL),\n" +
                        "(20, 4, FALSE, NULL);";

        SqlScriptRunner.execCommand(getConnection(),initRecordsScripts);
        DbInfoDto dbInfoDto = DbInfoExtractor.extract(getConnection());
        List<TableDto> tables = SqlDbHarvester.sortTablesByDependency(dbInfoDto);
        // TODO

    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public SutController getSutController() {
        return new DatabaseFakeMySQLSutController(connection);
    }
}
