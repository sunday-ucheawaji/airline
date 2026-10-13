-- Baseline schema for payment-service, generated from the Payment entity at the start of the money-correctness
-- pass. Amount is DECIMAL, not DOUBLE, since a payment amount is money. Dev databases are recreated, not migrated.

CREATE TABLE IF NOT EXISTS payments (
    id                  CHAR(36) PRIMARY KEY,
    -- Cross-service ref: User is in user-service, no FK.
    user_id             CHAR(36),
    -- Cross-service ref: Booking is in booking-service, no FK.
    booking_id          CHAR(36),
    -- Cross-service ref: Airline is in airline-core-service, no FK. Denormalized from the booking's own
    -- airlineId at initiatePayment time, so permission checks never need a Feign round trip per check.
    airline_id          CHAR(36),
    amount              DECIMAL(12,2),
    currency            VARCHAR(3),
    provider            ENUM('RAZORPAY','STRIPE','PAYSTACK'),
    provider_payment_id VARCHAR(255),
    transaction_id      VARCHAR(255),
    method              VARCHAR(255),
    status              ENUM('PENDING','SUCCESS','FAILED','CANCELLED','REFUNDED','PROCESSING'),
    failure_reason      VARCHAR(255),
    paid_at             DATETIME(6),
    refund_id           VARCHAR(255),
    refunded_amount     DECIMAL(12,2),
    idempotency_key         VARCHAR(255) UNIQUE,
    initiate_response_json  LONGTEXT,
    version                 BIGINT,
    created_at          DATETIME(6) NOT NULL,
    updated_at          DATETIME(6)
);

-- Inbox-pattern dedup for the Razorpay webhook: a redelivered event can never double-transition a
-- payment or double-publish payment.completed/payment.failed, even if it races a concurrent delivery.
CREATE TABLE IF NOT EXISTS processed_webhook_events (
    id              CHAR(36) PRIMARY KEY,
    provider        VARCHAR(50) NOT NULL,
    event_reference VARCHAR(255) NOT NULL,
    processed_at    DATETIME(6),
    CONSTRAINT uq_processed_webhook_events UNIQUE (provider, event_reference)
);

CREATE INDEX idx_payments_airline_id ON payments (airline_id);
