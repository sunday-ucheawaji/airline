CREATE TABLE IF NOT EXISTS onboarding_stage_reviews (
    id                CHAR(36) PRIMARY KEY,
    application_id    CHAR(36) NOT NULL,
    stage             ENUM('COMPLIANCE', 'COMMERCIAL', 'TECHNICAL') NOT NULL,
    status            ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    assignee_user_id  CHAR(36),
    comments          TEXT,
    decided_at        DATETIME(6),
    created_at        DATETIME(6) NOT NULL,
    updated_at        DATETIME(6) NOT NULL,
    CONSTRAINT uk_stage_reviews_application_stage UNIQUE (application_id, stage),
    CONSTRAINT fk_stage_reviews_application FOREIGN KEY (application_id) REFERENCES airline_onboarding_applications (id)
);
