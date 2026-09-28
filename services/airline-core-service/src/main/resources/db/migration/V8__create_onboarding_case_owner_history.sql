CREATE TABLE IF NOT EXISTS onboarding_case_owner_history (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id  BIGINT NOT NULL,
    from_user_id    BIGINT,
    to_user_id      BIGINT,
    actor_user_id   BIGINT NOT NULL,
    action          ENUM('CLAIMED', 'TRANSFERRED', 'RELEASED', 'TAKEN_OVER') NOT NULL,
    reason          TEXT,
    created_at      DATETIME(6) NOT NULL,
    CONSTRAINT fk_case_owner_history_application FOREIGN KEY (application_id) REFERENCES airline_onboarding_applications (id),
    INDEX idx_case_owner_history_application (application_id)
);
