CREATE TABLE IF NOT EXISTS stock_movements (
   id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
   item_id            BIGINT       NOT NULL,
   user_id            BIGINT       NOT NULL,
   type               VARCHAR(20)  NOT NULL,
   quantity           INT          NOT NULL,
   previous_quantity  INT          NOT NULL,
   new_quantity       INT          NOT NULL,
   reason             VARCHAR(255),
   reference          VARCHAR(100),
   created_at         DATETIME     NOT NULL,
   CONSTRAINT fk_movements_item FOREIGN KEY (item_id) REFERENCES items(id),
   CONSTRAINT fk_movements_user FOREIGN KEY (user_id) REFERENCES users(id),
   CONSTRAINT chk_movements_quantity CHECK (quantity > 0),
   INDEX idx_movements_item (item_id),
   INDEX idx_movements_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;