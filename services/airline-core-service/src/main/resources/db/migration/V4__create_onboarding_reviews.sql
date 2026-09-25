CREATE TABLE IF NOT EXISTS onboarding_reviews (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id     BIGINT NOT NULL,
    reviewer_user_id   BIGINT NOT NULL,
    decision           ENUM('APPROVED', 'REJECTED', 'REQUESTED_CHANGES', 'OWNER_ASSIGNED', 'PROVISIONED') NOT NULL,
    comments           TEXT,
    assigned_owner_user_id BIGINT,
    created_at         DATETIME(6) NOT NULL,
    CONSTRAINT fk_onboarding_reviews_application FOREIGN KEY (application_id) REFERENCES airline_onboarding_applications (id)
);
