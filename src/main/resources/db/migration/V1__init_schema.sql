CREATE TABLE users (
                       id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
                       username              VARCHAR(50)  NOT NULL UNIQUE,
                       password              VARCHAR(255) NOT NULL,
                       full_name             VARCHAR(100) NOT NULL,
                       email                 VARCHAR(100) NOT NULL,
                       role                  VARCHAR(20)  NOT NULL,
                       enabled               BOOLEAN      NOT NULL DEFAULT TRUE,
                       must_change_password  BOOLEAN      NOT NULL DEFAULT FALSE,
                       created_at            DATETIME     NOT NULL,
                       updated_at            DATETIME     NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE categories (
                            id           BIGINT AUTO_INCREMENT PRIMARY KEY,
                            name         VARCHAR(100) NOT NULL UNIQUE,
                            description  VARCHAR(255),
                            active       BOOLEAN      NOT NULL DEFAULT TRUE,
                            created_at   DATETIME     NOT NULL,
                            updated_at   DATETIME     NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;