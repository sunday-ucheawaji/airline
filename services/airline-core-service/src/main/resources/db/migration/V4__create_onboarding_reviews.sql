CREATE TABLE IF NOT EXISTS onboarding_reviews (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id     BIGINT NOT NULL,
    actor_user_id      BIGINT NOT NULL,
    decision           ENUM('APPROVED', 'REJECTED', 'REQUESTED_CHANGES', 'OWNER_ASSIGNED', 'PROVISIONED',
                            'COMMENTED', 'INFORMATION_REQUESTED', 'INFORMATION_PROVIDED', 'STAGE_ASSIGNED',
                            'STAGE_APPROVED', 'STAGE_REJECTED', 'REFERRED', 'SENT_BACK', 'WITHDRAWN',
                            'DOCUMENT_UPLOADED', 'DOCUMENT_VERIFIED', 'DOCUMENT_REJECTED') NOT NULL,
    comments           TEXT,
    target_user_id     BIGINT,
    stage              ENUM('COMPLIANCE', 'COMMERCIAL', 'TECHNICAL'),
    created_at         DATETIME(6) NOT NULL,
    CONSTRAINT fk_onboarding_reviews_application FOREIGN KEY (application_id) REFERENCES airline_onboarding_applications (id),
    INDEX idx_onboarding_reviews_application (application_id)
);
