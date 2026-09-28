CREATE TABLE IF NOT EXISTS onboarding_information_requests (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id        BIGINT NOT NULL,
    stage                 ENUM('COMPLIANCE', 'COMMERCIAL', 'TECHNICAL'),
    requested_by_user_id  BIGINT NOT NULL,
    message               TEXT NOT NULL,
    status                ENUM('OPEN', 'ANSWERED', 'CANCELLED') NOT NULL DEFAULT 'OPEN',
    response              TEXT,
    responded_at          DATETIME(6),
    created_at            DATETIME(6) NOT NULL,
    CONSTRAINT fk_information_requests_application FOREIGN KEY (application_id) REFERENCES airline_onboarding_applications (id),
    INDEX idx_information_requests_application_status (application_id, status)
);
