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
    fare_refundable         BIT(1),
    fare_cancellation_fee   DECIMAL(12,2),
    fare_refund_deadline_days INT,
    fare_changeable         BIT(1),
    fare_change_fee         DECIMAL(12,2),
    fare_change_deadline_hours INT,
    total_amount            DECIMAL(12,2),
    -- The change fee included in total_amount when this booking replaced another; never refunded.
    change_fee_amount       DECIMAL(12,2),
    refund_amount           DECIMAL(12,2),
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
    -- A voluntary change is a new booking: the replacement points at the original, and the original, once
    -- exchanged, at its replacement.
    -- The trip this flight belongs to (round trip or multi-city); null for a one-way booking.
    trip_id                 CHAR(36),
    exchanged_from_booking_id CHAR(36),
    exchanged_to_booking_id CHAR(36),
    version                 BIGINT,
    INDEX idx_bookings_exchanged_from (exchanged_from_booking_id),
    INDEX idx_bookings_trip (trip_id)
);

-- A round trip or multi-city journey: one booking per flight, paid with one payment whose payable id is the trip's.
CREATE TABLE IF NOT EXISTS trips (
    id              CHAR(36) PRIMARY KEY,
    trip_reference  VARCHAR(255) NOT NULL UNIQUE,
    user_id         CHAR(36) NOT NULL,
    -- Cross-service ref: Airline is in airline-core-service, no FK.
    airline_id      CHAR(36),
    trip_type       VARCHAR(20) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    currency        VARCHAR(3),
    total_amount    DECIMAL(12,2),
    -- Cross-service ref: Payment is in payment-service, no FK.
    payment_id      CHAR(36),
    version         BIGINT,
    created_at      DATETIME(6),
    updated_at      DATETIME(6)
);

-- Extras bought after a booking's purchase, each with its own payment whose payable id is the order's.
CREATE TABLE IF NOT EXISTS extras_orders (
    id              CHAR(36) PRIMARY KEY,
    booking_id      CHAR(36) NOT NULL,
    user_id         CHAR(36) NOT NULL,
    airline_id      CHAR(36) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    currency        VARCHAR(3) NOT NULL,
    total_amount    DECIMAL(12,2) NOT NULL,
    refund_amount   DECIMAL(12,2),
    idempotency_key VARCHAR(255),
    payment_id      CHAR(36),
    version         BIGINT,
    created_at      DATETIME(6),
    updated_at      DATETIME(6),
    INDEX idx_extras_orders_booking (booking_id),
    CONSTRAINT uq_extras_orders_idempotency UNIQUE (booking_id, idempotency_key),
    CONSTRAINT fk_extras_orders_booking FOREIGN KEY (booking_id) REFERENCES bookings (id)
);

CREATE TABLE IF NOT EXISTS extras_order_ancillaries (
    order_id        CHAR(36) NOT NULL,
    ancillary_id    CHAR(36),
    CONSTRAINT fk_extras_order_ancillaries_order FOREIGN KEY (order_id) REFERENCES extras_orders (id)
);

CREATE TABLE IF NOT EXISTS extras_order_meals (
    order_id        CHAR(36) NOT NULL,
    meal_id         CHAR(36),
    CONSTRAINT fk_extras_order_meals_order FOREIGN KEY (order_id) REFERENCES extras_orders (id)
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
    -- Cross-service ref: SeatInstance is in seat-service, no FK.
    seat_instance_id                CHAR(36),
    -- Age band on the day of travel, and the fare that priced this passenger (pricing-service, no FK).
    passenger_type                  ENUM('ADT','CHD','INF') NOT NULL,
    fare_id                         CHAR(36),
    fare_amount                     DECIMAL(12,2),
    fare_cancellation_fee           DECIMAL(12,2),
    fare_change_fee                 DECIMAL(12,2),
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
