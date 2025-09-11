INSERT INTO users (username, email) VALUES
                                        ('user01', 'user01@example.com'),
                                        ('user02', 'user02@example.com'),
                                        ('user03', 'user03@example.com'),
                                        ('user04', 'user04@example.com'),
                                        ('user05', 'user05@example.com'),
                                        ('user06', 'user06@example.com'),
                                        ('user07', 'user07@example.com'),
                                        ('user08', 'user08@example.com'),
                                        ('user09', 'user09@example.com'),
                                        ('user10', 'user10@example.com'),
                                        ('user11', 'user11@example.com'),
                                        ('user12', 'user12@example.com'),
                                        ('user13', 'user13@example.com'),
                                        ('user14', 'user14@example.com'),
                                        ('user15', 'user15@example.com'),
                                        ('user16', 'user16@example.com'),
                                        ('user17', 'user17@example.com'),
                                        ('user18', 'user18@example.com'),
                                        ('user19', 'user19@example.com'),
                                        ('user20', 'user20@example.com');

INSERT INTO categories (name, description) VALUES
                                               ('Electronics', 'Smartphones, laptops, accessories'),
                                               ('Books', 'Fiction, non-fiction, educational'),
                                               ('Clothing', 'Men, women, and children apparel'),
                                               ('Home & Kitchen', 'Furniture, utensils, decor'),
                                               ('Sports', 'Fitness equipment, sportswear');

INSERT INTO products (name, description, price, stock, category_id) VALUES
                                                                        ('iPhone 15', 'Latest Apple smartphone', 999.99, 50, 1),
                                                                        ('Samsung Galaxy S24', 'Android flagship', 899.99, 40, 1),
                                                                        ('MacBook Pro 16"', 'Powerful laptop for pros', 2499.99, 20, 1),
                                                                        ('Dell XPS 13', 'Ultra portable business laptop', 1199.99, 25, 1),
                                                                        ('iPad Air', 'Lightweight tablet', 599.99, 60, 1),
                                                                        ('Harry Potter Set', 'Complete 7-book collection', 89.99, 100, 2),
                                                                        ('The Alchemist', 'Paulo Coelho bestseller', 12.99, 200, 2),
                                                                        ('Atomic Habits', 'Self-improvement guide', 14.99, 150, 2),
                                                                        ('Dune', 'Sci-fi classic by Frank Herbert', 16.99, 120, 2),
                                                                        ('T-Shirt Cotton White', 'Comfortable everyday wear', 19.99, 300, 3),
                                                                        ('Jeans Slim Fit Blue', 'Classic denim jeans', 49.99, 150, 3),
                                                                        ('Running Shoes Nike', 'Lightweight for jogging', 89.99, 80, 3),
                                                                        ('Yoga Mat', 'Non-slip exercise mat', 29.99, 120, 5),
                                                                        ('Dumbbell Set 10kg', 'Adjustable weight set', 199.99, 40, 5),
                                                                        ('Coffee Maker', 'Automatic drip machine', 79.99, 70, 4),
                                                                        ('Air Fryer', 'Healthy oil-free frying', 129.99, 50, 4),
                                                                        ('Blender 1000W', 'Powerful kitchen blender', 59.99, 90, 4),
                                                                        ('Desk Lamp LED', 'Adjustable brightness', 35.99, 110, 4),
                                                                        ('Water Bottle 1L', 'Stainless steel insulated', 25.99, 200, 5),
                                                                        ('Backpack Waterproof', 'For hiking and travel', 69.99, 80, 5);

INSERT INTO user_addresses (user_id, address_line, city, postal_code, country, is_default) VALUES
                                                                                               (1, '123 Main St', 'Beijing', '100001', 'China', TRUE),
                                                                                               (2, '456 Oak Ave', 'Shanghai', '200001', 'China', TRUE),
                                                                                               (3, '789 Pine Rd', 'Guangzhou', '510001', 'China', FALSE),
                                                                                               (4, '321 Elm Blvd', 'Shenzhen', '518001', 'China', TRUE),
                                                                                               (5, '654 Cedar Ln', 'Hangzhou', '310001', 'China', TRUE),
                                                                                               (6, '987 Maple Dr', 'Chengdu', '610001', 'China', FALSE),
                                                                                               (7, '147 Birch St', 'Chongqing', '400001', 'China', TRUE),
                                                                                               (8, '258 Spruce Ave', 'Nanjing', '210001', 'China', TRUE),
                                                                                               (9, '369 Willow Rd', 'Wuhan', '430001', 'China', FALSE),
                                                                                               (10, '159 Aspen Blvd', 'Xi’an', '710001', 'China', TRUE),
                                                                                               (11, '357 Redwood Ln', 'Tianjin', '300001', 'China', TRUE),
                                                                                               (12, '753 Sequoia Dr', 'Suzhou', '215001', 'China', FALSE),
                                                                                               (13, '951 Fir St', 'Dalian', '116001', 'China', TRUE),
                                                                                               (14, '852 Cypress Ave', 'Qingdao', '266001', 'China', TRUE),
                                                                                               (15, '741 Palm Rd', 'Xiamen', '361001', 'China', FALSE),
                                                                                               (16, '630 Olive Blvd', 'Changsha', '410001', 'China', TRUE),
                                                                                               (17, '520 Cherry Ln', 'Zhengzhou', '450001', 'China', TRUE),
                                                                                               (18, '410 Peach Dr', 'Harbin', '150001', 'China', FALSE),
                                                                                               (19, '300 Plum St', 'Kunming', '650001', 'China', TRUE),
                                                                                               (20, '200 Orange Ave', 'Fuzhou', '350001', 'China', TRUE);

INSERT INTO orders (user_id, total_amount, status, order_date) VALUES
                                                                   (1, 1299.98, 'delivered', '2025-03-01 10:30:00'),
                                                                   (2, 89.99, 'shipped', '2025-03-02 14:15:00'),
                                                                   (3, 2499.99, 'paid', '2025-03-03 09:00:00'),
                                                                   (4, 109.98, 'pending', '2025-03-04 16:45:00'),
                                                                   (5, 199.99, 'delivered', '2025-02-28 11:20:00'),
                                                                   (6, 359.97, 'shipped', '2025-03-05 13:10:00'),
                                                                   (7, 79.99, 'delivered', '2025-03-01 17:50:00'),
                                                                   (8, 159.98, 'paid', '2025-03-06 08:30:00'),
                                                                   (9, 89.99, 'pending', '2025-03-07 12:00:00'),
                                                                   (10, 299.98, 'shipped', '2025-03-08 15:30:00'),
                                                                   (11, 59.99, 'delivered', '2025-03-02 10:00:00'),
                                                                   (12, 189.98, 'paid', '2025-03-09 14:20:00'),
                                                                   (13, 69.99, 'pending', '2025-03-10 09:45:00'),
                                                                   (14, 109.98, 'shipped', '2025-03-11 16:10:00'),
                                                                   (15, 2499.99, 'delivered', '2025-02-27 11:00:00'),
                                                                   (16, 149.98, 'paid', '2025-03-12 13:30:00'),
                                                                   (17, 129.99, 'pending', '2025-03-13 08:15:00'),
                                                                   (18, 39.98, 'shipped', '2025-03-14 17:00:00'),
                                                                   (19, 199.99, 'delivered', '2025-03-03 12:30:00'),
                                                                   (20, 89.99, 'paid', '2025-03-15 10:45:00');

INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES
                                                                         (1, 1, 1, 999.99),  -- iPhone
                                                                         (1, 6, 3, 89.99),   -- Books
                                                                         (2, 6, 1, 89.99),
                                                                         (3, 3, 1, 2499.99),
                                                                         (4, 10, 2, 19.99),  -- T-Shirts
                                                                         (4, 11, 1, 49.99),  -- Jeans
                                                                         (5, 13, 1, 199.99), -- Dumbbell
                                                                         (6, 16, 1, 129.99), -- Air Fryer
                                                                         (6, 17, 1, 59.99),  -- Blender
                                                                         (6, 18, 2, 35.99),  -- Lamps
                                                                         (7, 16, 1, 79.99),
                                                                         (8, 12, 1, 89.99),  -- Shoes
                                                                         (8, 14, 1, 29.99),  -- Yoga Mat
                                                                         (9, 12, 1, 89.99),
                                                                         (10, 2, 1, 899.99), -- Galaxy
                                                                         (10, 19, 1, 25.99), -- Bottle
                                                                         (11, 17, 1, 59.99),
                                                                         (12, 10, 5, 19.99), -- 5 T-Shirts
                                                                         (12, 11, 2, 49.99), -- 2 Jeans
                                                                         (13, 20, 1, 69.99), -- Backpack
                                                                         (14, 7, 2, 12.99),  -- 2x The Alchemist
                                                                         (14, 8, 2, 14.99),  -- 2x Atomic Habits
                                                                         (15, 3, 1, 2499.99),
                                                                         (16, 8, 3, 14.99),  -- 3x Atomic Habits
                                                                         (16, 9, 3, 16.99),  -- 3x Dune
                                                                         (17, 16, 1, 129.99),
                                                                         (18, 18, 1, 35.99), -- Lamp
                                                                         (18, 19, 1, 25.99), -- Bottle
                                                                         (19, 13, 1, 199.99),
                                                                         (20, 12, 1, 89.99);

INSERT INTO payments (order_id, amount, payment_method, payment_status, transaction_id, paid_at) VALUES
                                                                                                     (1, 1299.98, 'Credit Card', 'completed', 'txn_001', '2025-03-01 10:35:00'),
                                                                                                     (2, 89.99, 'Alipay', 'completed', 'txn_002', '2025-03-02 14:20:00'),
                                                                                                     (3, 2499.99, 'WeChat Pay', 'completed', 'txn_003', '2025-03-03 09:05:00'),
                                                                                                     (4, 109.98, 'Credit Card', 'pending', NULL, NULL),
                                                                                                     (5, 199.99, 'Alipay', 'completed', 'txn_005', '2025-02-28 11:25:00'),
                                                                                                     (6, 359.97, 'WeChat Pay', 'completed', 'txn_006', '2025-03-05 13:15:00'),
                                                                                                     (7, 79.99, 'Credit Card', 'completed', 'txn_007', '2025-03-01 17:55:00'),
                                                                                                     (8, 159.98, 'Alipay', 'completed', 'txn_008', '2025-03-06 08:35:00'),
                                                                                                     (9, 89.99, 'WeChat Pay', 'pending', NULL, NULL),
                                                                                                     (10, 299.98, 'Credit Card', 'completed', 'txn_010', '2025-03-08 15:35:00'),
                                                                                                     (11, 59.99, 'Alipay', 'completed', 'txn_011', '2025-03-02 10:05:00'),
                                                                                                     (12, 189.98, 'WeChat Pay', 'completed', 'txn_012', '2025-03-09 14:25:00'),
                                                                                                     (13, 69.99, 'Credit Card', 'pending', NULL, NULL),
                                                                                                     (14, 109.98, 'Alipay', 'completed', 'txn_014', '2025-03-11 16:15:00'),
                                                                                                     (15, 2499.99, 'Credit Card', 'completed', 'txn_015', '2025-02-27 11:05:00'),
                                                                                                     (16, 149.98, 'WeChat Pay', 'completed', 'txn_016', '2025-03-12 13:35:00'),
                                                                                                     (17, 129.99, 'Alipay', 'pending', NULL, NULL),
                                                                                                     (18, 39.98, 'Credit Card', 'completed', 'txn_018', '2025-03-14 17:05:00'),
                                                                                                     (19, 199.99, 'WeChat Pay', 'completed', 'txn_019', '2025-03-03 12:35:00'),
                                                                                                     (20, 89.99, 'Alipay', 'completed', 'txn_020', '2025-03-15 10:50:00');

INSERT INTO reviews (user_id, product_id, rating, comment) VALUES
                                                               (1, 1, 5, 'Amazing phone, worth every penny!'),
                                                               (2, 6, 4, 'Great book set for Harry Potter fans.'),
                                                               (3, 3, 5, 'Best laptop I have ever used.'),
                                                               (4, 10, 3, 'Comfortable but runs a bit small.'),
                                                               (5, 13, 4, 'Good quality dumbbells, solid build.'),
                                                               (6, 16, 5, 'Love my air fryer, cooks perfectly.'),
                                                               (7, 16, 4, 'Easy to use, cleans up nicely.'),
                                                               (8, 12, 5, 'Perfect for my morning runs.'),
                                                               (9, 12, 4, 'Good grip and cushioning.'),
                                                               (10, 2, 4, 'Great Android phone, battery life is good.'),
                                                               (11, 17, 3, 'Blender is okay, a bit noisy.'),
                                                               (12, 10, 5, 'Bought 5 for my team, all love them!'),
                                                               (13, 20, 4, 'Spacious and waterproof, great for travel.'),
                                                               (14, 7, 5, 'Life-changing book, highly recommend.'),
                                                               (15, 3, 5, 'Worth the investment for professionals.'),
                                                               (16, 8, 5, 'Helped me build better habits.'),
                                                               (17, 16, 4, 'Heats up fast, easy controls.'),
                                                               (18, 18, 3, 'Lamp is functional but design is plain.'),
                                                               (19, 13, 5, 'Perfect for home workouts.'),
                                                               (20, 12, 4, 'Comfortable for long walks too.');

INSERT INTO coupons (code, discount_percent, valid_from, valid_until, is_active) VALUES
                                                                                     ('WELCOME10', 10.00, '2025-01-01', '2025-12-31', TRUE),
                                                                                     ('SPRING20', 20.00, '2025-03-01', '2025-04-30', TRUE),
                                                                                     ('SUMMER25', 25.00, '2025-06-01', '2025-08-31', FALSE),
                                                                                     ('HOLIDAY30', 30.00, '2025-12-01', '2025-12-31', FALSE),
                                                                                     ('FLASH5', 5.00, '2025-03-15', '2025-03-16', TRUE),
                                                                                     ('VIP15', 15.00, '2025-01-01', '2025-12-31', TRUE),
                                                                                     ('NEWUSER10', 10.00, '2025-01-01', '2025-12-31', TRUE),
                                                                                     ('ELECTRO15', 15.00, '2025-03-01', '2025-03-31', TRUE),
                                                                                     ('BOOKS20', 20.00, '2025-03-01', '2025-03-31', TRUE),
                                                                                     ('FASHION10', 10.00, '2025-03-01', '2025-04-15', TRUE),
                                                                                     ('FITNESS25', 25.00, '2025-05-01', '2025-06-30', FALSE),
                                                                                     ('KITCHEN15', 15.00, '2025-03-01', '2025-04-30', TRUE),
                                                                                     ('BACKTOSCHOOL', 12.00, '2025-08-01', '2025-09-15', FALSE),
                                                                                     ('LOYALTY5', 5.00, '2025-01-01', '2025-12-31', TRUE),
                                                                                     ('FREESHIP', 0.00, '2025-03-01', '2025-03-31', TRUE); -- Free shipping coupon

INSERT INTO user_coupons (user_id, coupon_id, used, used_at) VALUES
                                                                 (1, 1, TRUE, '2025-03-01 10:30:00'),
                                                                 (2, 1, TRUE, '2025-03-02 14:15:00'),
                                                                 (3, 2, FALSE, NULL),
                                                                 (4, 3, FALSE, NULL),
                                                                 (5, 4, FALSE, NULL),
                                                                 (6, 5, TRUE, '2025-03-05 13:10:00'),
                                                                 (7, 6, TRUE, '2025-03-01 17:50:00'),
                                                                 (8, 7, TRUE, '2025-03-06 08:30:00'),
                                                                 (9, 8, FALSE, NULL),
                                                                 (10, 9, TRUE, '2025-03-08 15:30:00'),
                                                                 (11, 10, TRUE, '2025-03-02 10:00:00'),
                                                                 (12, 11, FALSE, NULL),
                                                                 (13, 12, FALSE, NULL),
                                                                 (14, 13, FALSE, NULL),
                                                                 (15, 14, TRUE, '2025-02-27 11:00:00'),
                                                                 (16, 15, TRUE, '2025-03-12 13:30:00'),
                                                                 (17, 1, TRUE, '2025-03-13 08:15:00'),
                                                                 (18, 2, FALSE, NULL),
                                                                 (19, 3, FALSE, NULL),
                                                                 (20, 4, FALSE, NULL);