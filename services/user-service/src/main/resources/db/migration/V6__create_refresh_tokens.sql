CREATE TABLE IF NOT EXISTS refresh_tokens (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT        NOT NULL,
    token_hash    VARCHAR(255)  NOT NULL,
    expires_at    DATETIME      NOT NULL,
    revoked_at    DATETIME,
    last_used_at  DATETIME,
    user_agent    VARCHAR(500),
    ip_address    VARCHAR(100),
    created_at    DATETIME      NOT NULL,
    CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
);
