CREATE TABLE IF NOT EXISTS users (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name     VARCHAR(100)  NOT NULL,
    last_name      VARCHAR(100)  NOT NULL,
    middle_name    VARCHAR(100),
    password       VARCHAR(255),
    email          VARCHAR(255)  NOT NULL,
    phone_number   VARCHAR(30),
    email_verified BOOLEAN       NOT NULL DEFAULT FALSE,
    status                VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    failed_login_attempts INT         NOT NULL DEFAULT 0,
    locked_until          DATETIME    NULL,
    last_login     DATETIME,
    created_at     DATETIME      NOT NULL,
    updated_at     DATETIME      NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email)
);
