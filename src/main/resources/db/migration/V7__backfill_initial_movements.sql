-- For every existing item that has quantity > 0 but no stock movements,
-- insert a single "Initial stock (backfill)" STOCK_IN movement.
-- This brings pre-Phase-6 items into alignment with the new movement ledger.
INSERT INTO stock_movements
(item_id, user_id, type, quantity, previous_quantity, new_quantity, reason, reference, created_at)
SELECT
    i.id,
    (SELECT id FROM users WHERE role = 'ADMIN' ORDER BY id LIMIT 1),
    'STOCK_IN',
    i.quantity,
    0,
    i.quantity,
    'Initial stock (backfill)',
    NULL,
    i.created_at
FROM items i
WHERE i.quantity > 0
  AND NOT EXISTS (
    SELECT 1 FROM stock_movements m WHERE m.item_id = i.id
);