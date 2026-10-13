CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id          CHAR(36) PRIMARY KEY,
    user_id     CHAR(36) NOT NULL,
    token_hash  VARCHAR(255) NOT NULL,
    expires_at  DATETIME NOT NULL,
    used_at     DATETIME,
    created_at  DATETIME NOT NULL,
    CONSTRAINT uk_password_reset_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
);
