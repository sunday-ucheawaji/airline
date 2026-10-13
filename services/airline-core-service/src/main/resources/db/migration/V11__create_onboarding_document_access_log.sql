-- No FK to onboarding_documents: the access trail must outlive a document the applicant deletes from a draft.
CREATE TABLE IF NOT EXISTS onboarding_document_access_log (
    id              CHAR(36) PRIMARY KEY,
    document_id     CHAR(36) NOT NULL,
    application_id  CHAR(36) NOT NULL,
    user_id         CHAR(36) NOT NULL,
    accessor        ENUM('APPLICANT', 'STAFF') NOT NULL,
    accessed_at     DATETIME(6) NOT NULL,
    INDEX idx_document_access_document (document_id, accessed_at)
);
