-- Outbox pattern: an event is written here in the SAME transaction as the business data change
-- it describes, so the two either both commit or both roll back together. A separate sweeper
-- delivers PENDING rows to Kafka asynchronously and retries on failure, instead of the previous
-- approach of calling KafkaTemplate.send() directly inside the transaction (a dual-write: the
-- event could be published even though the surrounding transaction later rolled back, or lost
-- silently on a broker outage since KafkaTemplate.send() is asynchronous and nothing checked the
-- returned future).
CREATE TABLE IF NOT EXISTS outbox_events (
    id               CHAR(36) PRIMARY KEY,
    topic            VARCHAR(100) NOT NULL,
    event_type       VARCHAR(255) NOT NULL,
    payload          TEXT NOT NULL,
    status           VARCHAR(20) NOT NULL,
    attempts         INT NOT NULL DEFAULT 0,
    last_error       VARCHAR(1000),
    created_at       DATETIME(6) NOT NULL,
    last_attempt_at  DATETIME(6),
    INDEX idx_outbox_events_status_created (status, created_at)
);
