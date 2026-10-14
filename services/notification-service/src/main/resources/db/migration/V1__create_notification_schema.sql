-- Baseline schema for notification-service. One row per (event_type, reference, channel) — doubles as
-- the delivery-tracking record and the idempotency claim for that send. Dev DBs are recreated, not
-- migrated, same convention as every other service's V1 baseline in this repo.

CREATE TABLE IF NOT EXISTS notifications (
    id                  CHAR(36) PRIMARY KEY,
    event_type          ENUM('BOOKING_CONFIRMED','BOOKING_CANCELLED','PAYMENT_FAILED','PAYMENT_REFUNDED','EXTRAS_CONFIRMED','EMAIL_VERIFICATION','PASSWORD_RESET') NOT NULL,
    reference           VARCHAR(100) NOT NULL,
    channel             ENUM('EMAIL','SMS') NOT NULL,
    recipient           VARCHAR(255) NOT NULL,
    status              ENUM('PENDING','SENT','FAILED') NOT NULL,
    subject             VARCHAR(255),
    body                LONGTEXT,
    provider_message_id VARCHAR(255),
    failure_reason      VARCHAR(500),
    attempt_count       INT NOT NULL DEFAULT 0,
    version             BIGINT,
    created_at          DATETIME(6),
    updated_at          DATETIME(6),
    sent_at             DATETIME(6),
    CONSTRAINT uq_notifications_event_reference_channel UNIQUE (event_type, reference, channel)
);

CREATE INDEX idx_notifications_reference ON notifications (reference);
