CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id          CHAR(36)    PRIMARY KEY,
    user_id     CHAR(36)    NOT NULL,
    token_hash  VARCHAR(255)  NOT NULL,
    expires_at  DATETIME      NOT NULL,
    used_at     DATETIME,
    created_at  DATETIME      NOT NULL,
    CONSTRAINT uk_email_verification_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_email_verification_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
);
