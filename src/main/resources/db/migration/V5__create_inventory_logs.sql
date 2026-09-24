CREATE TABLE IF NOT EXISTS inventory_logs (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  entity_type  VARCHAR(20)  NOT NULL,
  entity_id    BIGINT       NOT NULL,
  entity_name  VARCHAR(200) NOT NULL,
  action       VARCHAR(20)  NOT NULL,
  user_id      BIGINT       NOT NULL,
  message      VARCHAR(500) NOT NULL,
  created_at   DATETIME     NOT NULL,
  CONSTRAINT fk_logs_user FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_logs_entity (entity_type, entity_id),
  INDEX idx_logs_created_at (created_at),
  INDEX idx_logs_action (action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;