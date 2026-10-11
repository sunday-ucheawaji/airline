-- Baseline schema for booking-service. Matches the entities at the start of the booking pass, including the fare
-- snapshot columns. Later steps in the pass edit this file in place, as every other service's V1 does.

CREATE TABLE IF NOT EXISTS bookings (
    id                      CHAR(36) PRIMARY KEY,
    booking_reference       VARCHAR(255) NOT NULL UNIQUE,
    user_id                 CHAR(36),
    -- Cross-service ref: Flight and FlightInstance are in flight-ops-service, no FK.
    flight_id               CHAR(36),
    flight_instance_id      CHAR(36),
    -- Cross-service ref: Airline is in airline-core-service, no FK.
    airline_id              CHAR(36) NOT NULL,
    trip_type               TINYINT CHECK (trip_type BETWEEN 0 AND 2),
    cabin_class             ENUM('ECONOMY','PREMIUM_ECONOMY','BUSINESS','FIRST'),
    -- Cross-service ref: Fare is in pricing-service, no FK. The snapshot columns record the fare as priced.
    fare_id                 CHAR(36),
    fare_version            BIGINT,
    fare_currency           VARCHAR(255),
    fare_base_amount        DECIMAL(12,2),
    fare_tax_amount         DECIMAL(12,2),
    fare_fee_amount         DECIMAL(12,2),
    fare_rules_id           CHAR(36),
    fare_baggage_policy_id  CHAR(36),
    fare_travel_date        DATE,
    total_amount            DECIMAL(12,2),
    flexible_ticket         BIT(1) NOT NULL,
    ticket_time_limit       DATETIME(6),
    ticket_issued           BIT(1) NOT NULL,
    -- Cross-service ref: Payment is in payment-service, no FK.
    payment_id              CHAR(36),
    status                  ENUM('CANCELLED','COMPLETED','CONFIRMED','PENDING'),
    booking_date            DATETIME(6),
    last_modified           DATETIME(6),
    email                   VARCHAR(255),
    phone                   VARCHAR(255),
    version                 BIGINT
);

CREATE TABLE IF NOT EXISTS passengers (
    id                              CHAR(36) PRIMARY KEY,
    booking_id                      CHAR(36),
    -- Cross-service ref: the traveller account that created the record. Records are never matched across users.
    primary_user_id                 CHAR(36),
    first_name                      VARCHAR(255) NOT NULL,
    last_name                       VARCHAR(255) NOT NULL,
    gender                          ENUM('FEMALE','MALE','OTHER') NOT NULL,
    date_of_birth                   DATE NOT NULL,
    nationality                     VARCHAR(255),
    passport_number                 VARCHAR(255),
    email                           VARCHAR(255),
    phone                           VARCHAR(255),
    frequent_flyer_number           VARCHAR(255),
    dietary_preferences             VARCHAR(255),
    medical_conditions              VARCHAR(255),
    requires_wheelchair_assistance  BIT(1),
    is_active                       BIT(1) NOT NULL,
    version                         BIGINT,
    created_at                      DATETIME(6),
    updated_at                      DATETIME(6),
    CONSTRAINT fk_passengers_booking FOREIGN KEY (booking_id) REFERENCES bookings (id)
);

CREATE TABLE IF NOT EXISTS tickets (
    id              CHAR(36) PRIMARY KEY,
    booking_id      CHAR(36),
    passenger_id    CHAR(36),
    ticket_number   VARCHAR(255) NOT NULL UNIQUE,
    status          ENUM('BOOKED','CANCELLED','EXPIRED','REFUNDED','USED'),
    issued_at       DATETIME(6),
    CONSTRAINT fk_tickets_booking FOREIGN KEY (booking_id) REFERENCES bookings (id),
    CONSTRAINT fk_tickets_passenger FOREIGN KEY (passenger_id) REFERENCES passengers (id)
);

CREATE TABLE IF NOT EXISTS booking_ancillaries (
    booking_id      CHAR(36) NOT NULL,
    ancillary_id    CHAR(36),
    CONSTRAINT fk_booking_ancillaries_booking FOREIGN KEY (booking_id) REFERENCES bookings (id)
);

CREATE TABLE IF NOT EXISTS booking_meals (
    booking_id      CHAR(36) NOT NULL,
    meal_id         CHAR(36),
    CONSTRAINT fk_booking_meals_booking FOREIGN KEY (booking_id) REFERENCES bookings (id)
);

CREATE TABLE IF NOT EXISTS booking_seat_instances (
    booking_id          CHAR(36) NOT NULL,
    seat_instance_id    CHAR(36),
    CONSTRAINT fk_booking_seat_instances_booking FOREIGN KEY (booking_id) REFERENCES bookings (id)
);

-- Durable saga-progress record for one createBooking attempt, keyed by the caller's Idempotency-Key
-- (or a server-generated one when absent). See BookingSagaService / BookingSagaRecoverySweeper.
CREATE TABLE IF NOT EXISTS booking_sagas (
    id                  CHAR(36) PRIMARY KEY,
    idempotency_key     VARCHAR(255) NOT NULL,
    request_hash        VARCHAR(64) NOT NULL,
    user_id             CHAR(36) NOT NULL,
    booking_reference   VARCHAR(255),
    booking_id          CHAR(36),
    status              ENUM('STARTED','SEATS_HELD','BOOKING_CREATED','PAYMENT_INITIATED','COMPLETED','CANCELLED','FAILED'),
    response_json       LONGTEXT,
    last_error          VARCHAR(500),
    version             BIGINT,
    created_at          DATETIME(6),
    updated_at          DATETIME(6),
    CONSTRAINT uq_booking_sagas_user_key UNIQUE (user_id, idempotency_key)
);

CREATE TABLE IF NOT EXISTS booking_saga_seat_instances (
    booking_saga_id     CHAR(36) NOT NULL,
    seat_instance_id    CHAR(36),
    CONSTRAINT fk_booking_saga_seat_instances_saga FOREIGN KEY (booking_saga_id) REFERENCES booking_sagas (id)
);

-- Inbox-pattern dedup for PaymentEventListener: a redelivered payment.completed/payment.failed can
-- never double-apply its side effect, even if it races a concurrent delivery.
CREATE TABLE IF NOT EXISTS processed_payment_events (
    id              CHAR(36) PRIMARY KEY,
    payment_id      CHAR(36) NOT NULL,
    event_type      VARCHAR(50) NOT NULL,
    processed_at    DATETIME(6),
    CONSTRAINT uq_processed_payment_events UNIQUE (payment_id, event_type)
);
