CREATE TABLE IF NOT EXISTS onboarding_case_owner_history (
    id              CHAR(36) PRIMARY KEY,
    application_id  CHAR(36) NOT NULL,
    from_user_id    CHAR(36),
    to_user_id      CHAR(36),
    actor_user_id   CHAR(36) NOT NULL,
    action          ENUM('CLAIMED', 'TRANSFERRED', 'RELEASED', 'TAKEN_OVER') NOT NULL,
    reason          TEXT,
    created_at      DATETIME(6) NOT NULL,
    CONSTRAINT fk_case_owner_history_application FOREIGN KEY (application_id) REFERENCES airline_onboarding_applications (id),
    INDEX idx_case_owner_history_application (application_id)
);
