-- Sample products for the online shopping system.
-- INSERT IGNORE keeps seeding idempotent (safe for MySQL restarts).
INSERT IGNORE INTO products (id, name, description, price, quantity, image_url, category, available, created_at) VALUES
(1, 'Wireless Mouse', 'Ergonomic 2.4GHz wireless mouse with silent clicks and 1600 DPI adjustable sensor.', 24.99, 100, 'https://placehold.co/400x300/4f9da6/fff?text=Wireless+Mouse', 'Electronics', true, NOW()),
(2, 'Java Programming Book', 'A comprehensive guide to modern Java development with practical examples.', 45.00, 50, 'https://placehold.co/400x300/6c4fa6/fff?text=Java+Book', 'Books', true, NOW()),
(3, 'Cotton T-Shirt', 'Soft premium 100% cotton t-shirt, available in multiple sizes.', 19.99, 200, 'https://placehold.co/400x300/a64f6c/fff?text=Cotton+T-Shirt', 'Clothing', true, NOW()),
(4, 'Leather Wallet', 'Genuine leather bi-fold wallet with RFID blocking protection.', 29.50, 75, 'https://placehold.co/400x300/a6844f/fff?text=Leather+Wallet', 'Accessories', true, NOW()),
(5, 'Ceramic Mug Set', 'Set of 4 dishwasher-safe ceramic mugs in assorted colours.', 15.75, 120, 'https://placehold.co/400x300/4fa67a/fff?text=Mug+Set', 'Home', true, NOW());