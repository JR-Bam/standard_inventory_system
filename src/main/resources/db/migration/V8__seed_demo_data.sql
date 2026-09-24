-- =====================================================================
-- Demo seed data. Runs only when the categories table is empty.
-- Safe to leave in place — on a populated DB, it inserts nothing.
-- =====================================================================

-- ---------------- Categories ----------------
INSERT INTO categories (name, description, active, created_at, updated_at)
SELECT * FROM (
                  SELECT 'Laptops'      AS name, 'Portable computers'           AS description, TRUE AS active, NOW() AS created_at, NOW() AS updated_at
                  UNION ALL SELECT 'Accessories', 'Mice, keyboards, bags',       TRUE, NOW(), NOW()
                  UNION ALL SELECT 'Cables',      'USB, HDMI, power cables',     TRUE, NOW(), NOW()
                  UNION ALL SELECT 'Monitors',    'External displays',           TRUE, NOW(), NOW()
                  UNION ALL SELECT 'Storage',     'External drives, SSDs',       TRUE, NOW(), NOW()
              ) AS seed
WHERE (SELECT COUNT(*) FROM categories) = 0;

-- ---------------- Items ----------------
-- Insert demo items only if no items exist yet.
-- quantity is set here; a matching movement is inserted below.
INSERT INTO items (sku, name, description, category_id, quantity, unit_price, reorder_level, location, active, created_by, created_at, updated_at)
SELECT * FROM (
                  SELECT 'LAP-001' AS sku, 'Dell XPS 13' AS name, '13-inch ultrabook, 16GB RAM' AS description,
                         (SELECT id FROM categories WHERE name = 'Laptops') AS category_id,
                         12 AS quantity, 85000.00 AS unit_price, 3 AS reorder_level,
                         'Warehouse A / Shelf 1' AS location, TRUE AS active,
                         (SELECT id FROM users WHERE role = 'ADMIN' ORDER BY id LIMIT 1) AS created_by,
                         NOW() AS created_at, NOW() AS updated_at
                  UNION ALL SELECT 'LAP-002', 'MacBook Pro 14', 'M3 Pro, 18GB RAM',
                                   (SELECT id FROM categories WHERE name = 'Laptops'),
                                   4, 140000.00, 3, 'Warehouse A / Shelf 1', TRUE,
                                   (SELECT id FROM users WHERE role = 'ADMIN' ORDER BY id LIMIT 1), NOW(), NOW()
                  UNION ALL SELECT 'LAP-003', 'ThinkPad X1 Carbon', 'Business ultrabook',
                                   (SELECT id FROM categories WHERE name = 'Laptops'),
                                   0, 95000.00, 2, 'Warehouse A / Shelf 1', TRUE,
                                   (SELECT id FROM users WHERE role = 'ADMIN' ORDER BY id LIMIT 1), NOW(), NOW()
                  UNION ALL SELECT 'ACC-001', 'Logitech MX Master 3', 'Wireless ergonomic mouse',
                                   (SELECT id FROM categories WHERE name = 'Accessories'),
                                   45, 5500.00, 10, 'Warehouse B / Bin 3', TRUE,
                                   (SELECT id FROM users WHERE role = 'ADMIN' ORDER BY id LIMIT 1), NOW(), NOW()
                  UNION ALL SELECT 'ACC-002', 'Keychron K2 Keyboard', 'Mechanical, wireless',
                                   (SELECT id FROM categories WHERE name = 'Accessories'),
                                   18, 6200.00, 8, 'Warehouse B / Bin 3', TRUE,
                                   (SELECT id FROM users WHERE role = 'ADMIN' ORDER BY id LIMIT 1), NOW(), NOW()
                  UNION ALL SELECT 'ACC-003', 'Laptop Sleeve 14"', 'Padded neoprene sleeve',
                                   (SELECT id FROM categories WHERE name = 'Accessories'),
                                   6, 1200.00, 10, 'Warehouse B / Bin 4', TRUE,
                                   (SELECT id FROM users WHERE role = 'ADMIN' ORDER BY id LIMIT 1), NOW(), NOW()
                  UNION ALL SELECT 'CBL-001', 'USB-C Cable 2m', 'USB-C to USB-C, 100W',
                                   (SELECT id FROM categories WHERE name = 'Cables'),
                                   120, 450.00, 30, 'Warehouse C / Drawer 1', TRUE,
                                   (SELECT id FROM users WHERE role = 'ADMIN' ORDER BY id LIMIT 1), NOW(), NOW()
                  UNION ALL SELECT 'CBL-002', 'HDMI Cable 2m', 'HDMI 2.1, 8K capable',
                                   (SELECT id FROM categories WHERE name = 'Cables'),
                                   25, 800.00, 15, 'Warehouse C / Drawer 1', TRUE,
                                   (SELECT id FROM users WHERE role = 'ADMIN' ORDER BY id LIMIT 1), NOW(), NOW()
                  UNION ALL SELECT 'MON-001', 'LG UltraFine 27"', '27-inch 4K IPS',
                                   (SELECT id FROM categories WHERE name = 'Monitors'),
                                   8, 32000.00, 2, 'Warehouse A / Shelf 4', TRUE,
                                   (SELECT id FROM users WHERE role = 'ADMIN' ORDER BY id LIMIT 1), NOW(), NOW()
                  UNION ALL SELECT 'STO-001', 'Samsung T7 1TB SSD', 'Portable USB-C SSD',
                                   (SELECT id FROM categories WHERE name = 'Storage'),
                                   3, 7800.00, 5, 'Warehouse C / Safe 1', TRUE,
                                   (SELECT id FROM users WHERE role = 'ADMIN' ORDER BY id LIMIT 1), NOW(), NOW()
              ) AS seed
WHERE (SELECT COUNT(*) FROM items) = 0;

-- ---------------- Initial Stock Movements ----------------
-- Create an "Initial stock" movement for every seeded item.
-- Only for items that don't already have any movements.
INSERT INTO stock_movements
(item_id, user_id, type, quantity, previous_quantity, new_quantity, reason, reference, created_at)
SELECT
    i.id,
    i.created_by,
    'STOCK_IN',
    i.quantity,
    0,
    i.quantity,
    'Initial stock',
    NULL,
    i.created_at
FROM items i
WHERE i.quantity > 0
  AND NOT EXISTS (SELECT 1 FROM stock_movements m WHERE m.item_id = i.id);

-- ---------------- Historical Movements ----------------
-- Sprinkle some inbound/outbound activity across the last 14 days so the
-- dashboard movement chart has visible trends. Only if few movements exist.
INSERT INTO stock_movements
(item_id, user_id, type, quantity, previous_quantity, new_quantity, reason, reference, created_at)
SELECT
    i.id,
    i.created_by,
    m.type,
    m.qty,
    0,
    m.qty,
    m.reason,
    m.ref,
    DATE_SUB(NOW(), INTERVAL m.days_ago DAY)
FROM (
         SELECT 'LAP-001' AS sku, 'STOCK_IN'  AS type, 5 AS qty, 12 AS days_ago, 'Restock' AS reason, 'PO-2026-0401' AS ref
         UNION ALL SELECT 'LAP-001', 'STOCK_OUT', 3, 10, 'Sale', 'INV-1001'
         UNION ALL SELECT 'LAP-001', 'STOCK_OUT', 2,  7, 'Sale', 'INV-1015'
         UNION ALL SELECT 'ACC-001', 'STOCK_IN', 20, 14, 'Restock', 'PO-2026-0402'
         UNION ALL SELECT 'ACC-001', 'STOCK_OUT', 8, 9, 'Sale', 'INV-1020'
         UNION ALL SELECT 'ACC-001', 'STOCK_OUT', 5, 4, 'Sale', 'INV-1033'
         UNION ALL SELECT 'CBL-001', 'STOCK_IN', 50, 8, 'Restock', 'PO-2026-0403'
         UNION ALL SELECT 'CBL-001', 'STOCK_OUT', 15, 6, 'Sale', 'INV-1041'
         UNION ALL SELECT 'CBL-002', 'STOCK_OUT', 4, 5, 'Damaged', NULL
         UNION ALL SELECT 'MON-001', 'STOCK_IN', 3, 6, 'Restock', 'PO-2026-0404'
         UNION ALL SELECT 'MON-001', 'STOCK_OUT', 1, 3, 'Sale', 'INV-1050'
         UNION ALL SELECT 'ACC-002', 'STOCK_IN', 10, 11, 'Restock', 'PO-2026-0405'
         UNION ALL SELECT 'ACC-002', 'STOCK_OUT', 2, 2, 'Sale', 'INV-1060'
     ) AS m
         JOIN items i ON i.sku = m.sku
WHERE (SELECT COUNT(*) FROM stock_movements) < 15;