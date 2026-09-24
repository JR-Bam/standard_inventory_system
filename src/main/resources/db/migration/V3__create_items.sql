CREATE TABLE IF NOT EXISTS items (
                       id             BIGINT AUTO_INCREMENT PRIMARY KEY,
                       sku            VARCHAR(50)   NOT NULL UNIQUE,
                       name           VARCHAR(150)  NOT NULL,
                       description    VARCHAR(500),
                       category_id    BIGINT        NOT NULL,
                       quantity       INT           NOT NULL DEFAULT 0,
                       unit_price     DECIMAL(12,2) NOT NULL DEFAULT 0.00,
                       reorder_level  INT           NOT NULL DEFAULT 0,
                       location       VARCHAR(100),
                       active         BOOLEAN       NOT NULL DEFAULT TRUE,
                       created_by     BIGINT,
                       created_at     DATETIME      NOT NULL,
                       updated_at     DATETIME      NOT NULL,
                       CONSTRAINT fk_items_category FOREIGN KEY (category_id) REFERENCES categories(id),
                       CONSTRAINT fk_items_created_by FOREIGN KEY (created_by) REFERENCES users(id),
                       CONSTRAINT chk_items_quantity CHECK (quantity >= 0),
                       CONSTRAINT chk_items_price CHECK (unit_price >= 0),
                       CONSTRAINT chk_items_reorder CHECK (reorder_level >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_items_sku ON items(sku);
CREATE INDEX idx_items_active ON items(active);
CREATE INDEX idx_items_category ON items(category_id);