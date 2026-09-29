CREATE TABLE IF NOT EXISTS airline_memberships (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    airline_id          BIGINT NOT NULL,
    user_id             BIGINT NOT NULL,
    role_id             BIGINT NOT NULL,
    status              ENUM('INVITED', 'ACTIVE', 'SUSPENDED', 'REMOVED', 'REVOKED', 'EXPIRED') NOT NULL DEFAULT 'ACTIVE',
    invited_by_user_id  BIGINT,
    expires_at          DATETIME(6),
    joined_at           DATETIME(6),
    created_at          DATETIME(6) NOT NULL,
    updated_at          DATETIME(6) NOT NULL,
    INDEX idx_airline_memberships_user (user_id),
    INDEX idx_airline_memberships_status_expires (status, expires_at),
    CONSTRAINT uk_airline_memberships_user_airline UNIQUE (user_id, airline_id),
    CONSTRAINT fk_airline_memberships_airline FOREIGN KEY (airline_id) REFERENCES airlines (id)
);
