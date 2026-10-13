-- Baseline schema for seat-service, captured after the RBAC/exception/state-machine hardening
-- pass (no pre-existing dev data to migrate — dev DBs are dropped and recreated, same convention
-- as every other service's V1 baseline in this repo). Formalizes what Hibernate was previously
-- auto-generating, now that ddl-auto is `validate`.
--
-- Dropped versus the pre-hardening shape: FlightInstanceCabin.bookedSeats/getAvailableSeats()
-- (dead counter, never incremented — real availability is tracked per-SeatInstance.status) and
-- SeatInstance.isAvailable/isBooked (legacy booleans, superseded by status, went stale on
-- OCCUPIED/BLOCKED transitions).

CREATE TABLE IF NOT EXISTS cabin_classes (
    id                  CHAR(36) PRIMARY KEY,
    name                ENUM('ECONOMY','PREMIUM_ECONOMY','BUSINESS','FIRST') NOT NULL,
    code                VARCHAR(5) NOT NULL,
    description         VARCHAR(255),
    -- Cross-service ref: Aircraft is in airline-core-service, no FK.
    aircraft_id         CHAR(36),
    display_order       INT NOT NULL,
    is_active           BOOLEAN NOT NULL,
    is_bookable         BOOLEAN NOT NULL,
    typical_seat_pitch  INT,
    typical_seat_width  INT,
    seat_type           ENUM('WINDOW','MIDDLE','AISLE'),
    created_at          DATETIME(6) NOT NULL,
    updated_at          DATETIME(6) NOT NULL
);

CREATE INDEX idx_cabin_classes_aircraft_id ON cabin_classes (aircraft_id);
CREATE UNIQUE INDEX uk_cabin_classes_code_aircraft ON cabin_classes (code, aircraft_id);

CREATE TABLE IF NOT EXISTS seat_maps (
    id                      CHAR(36) PRIMARY KEY,
    name                    VARCHAR(255) NOT NULL,
    total_rows              INT NOT NULL,
    right_seats_per_row     INT NOT NULL,
    left_seats_per_row      INT NOT NULL,
    -- Cross-service ref: Airline is in airline-core-service, no FK.
    airline_id              CHAR(36) NOT NULL,
    cabin_class_id          CHAR(36),
    CONSTRAINT uk_seat_maps_cabin_class UNIQUE (cabin_class_id),
    CONSTRAINT fk_seat_maps_cabin_class FOREIGN KEY (cabin_class_id) REFERENCES cabin_classes (id)
);

CREATE INDEX idx_seat_maps_airline_id ON seat_maps (airline_id);

CREATE TABLE IF NOT EXISTS seats (
    id                          CHAR(36) PRIMARY KEY,
    seat_number                 VARCHAR(10) NOT NULL,
    seat_row                    INT NOT NULL,
    column_letter               CHAR(1),
    seat_type                   VARCHAR(20) NOT NULL,
    base_price                  DECIMAL(12,2),
    premium_surcharge           DECIMAL(12,2),
    is_available                BOOLEAN NOT NULL,
    is_blocked                  BOOLEAN NOT NULL,
    is_emergency_exit           BOOLEAN NOT NULL,
    is_active                   BOOLEAN NOT NULL,
    has_extra_legroom           BOOLEAN NOT NULL,
    has_bassinet                BOOLEAN NOT NULL,
    is_near_lavatory            BOOLEAN NOT NULL,
    is_near_galley              BOOLEAN NOT NULL,
    has_power_outlet            BOOLEAN NOT NULL,
    has_tv_screen               BOOLEAN NOT NULL,
    is_wheelchair_accessible    BOOLEAN NOT NULL,
    has_extra_width             BOOLEAN NOT NULL,
    seat_pitch                  INT,
    seat_width                  INT,
    recline_angle               INT,
    seat_map_id                 CHAR(36) NOT NULL,
    cabin_class_id              CHAR(36),
    created_at                  DATETIME(6) NOT NULL,
    updated_at                  DATETIME(6) NOT NULL,
    created_by                  VARCHAR(255),
    updated_by                  VARCHAR(255),
    version                     BIGINT,
    CONSTRAINT fk_seats_seat_map FOREIGN KEY (seat_map_id) REFERENCES seat_maps (id),
    CONSTRAINT fk_seat_cabinclass FOREIGN KEY (cabin_class_id) REFERENCES cabin_classes (id)
);

CREATE INDEX idx_seats_seat_map_id ON seats (seat_map_id);
CREATE INDEX idx_seats_cabin_class_id ON seats (cabin_class_id);

CREATE TABLE IF NOT EXISTS flight_instance_cabins (
    id                   CHAR(36) PRIMARY KEY,
    -- Cross-service ref: FlightInstance is in flight-ops-service, no FK.
    flight_instance_id   CHAR(36) NOT NULL,
    cabin_class_id       CHAR(36) NOT NULL,
    total_seats          INT NOT NULL,
    CONSTRAINT fk_flight_instance_cabins_cabin_class FOREIGN KEY (cabin_class_id) REFERENCES cabin_classes (id)
);

CREATE INDEX idx_flight_instance_cabins_flight_instance_id ON flight_instance_cabins (flight_instance_id);
CREATE INDEX idx_flight_instance_cabins_cabin_class_id ON flight_instance_cabins (cabin_class_id);

CREATE TABLE IF NOT EXISTS seat_instances (
    id                          CHAR(36) PRIMARY KEY,
    -- Cross-service refs: Flight/FlightInstance/FlightSchedule are in flight-ops-service, no FK.
    flight_id                   CHAR(36) NOT NULL,
    flight_instance_cabin_id    CHAR(36),
    flight_instance_id          CHAR(36),
    seat_id                     CHAR(36) NOT NULL,
    status                      ENUM('AVAILABLE','HELD','BOOKED','OCCUPIED','BLOCKED') NOT NULL,
    meal_preference              VARCHAR(255),
    fare                         DECIMAL(12,2),
    premium_surcharge            DECIMAL(12,2),
    held_until                   DATETIME(6),
    booking_reference            VARCHAR(255),
    version                      BIGINT,
    flight_schedule_id           CHAR(36),
    created_at                   DATETIME(6),
    updated_at                   DATETIME(6),
    CONSTRAINT fk_seat_instances_flight_instance_cabin FOREIGN KEY (flight_instance_cabin_id) REFERENCES flight_instance_cabins (id),
    CONSTRAINT fk_seat_instances_seat FOREIGN KEY (seat_id) REFERENCES seats (id),
    CONSTRAINT uk_seat_instances_seat_flight_instance UNIQUE (seat_id, flight_instance_id)
);

CREATE INDEX idx_seat_instances_flight_id ON seat_instances (flight_id);
CREATE INDEX idx_seat_instances_flight_instance_id ON seat_instances (flight_instance_id);
CREATE INDEX idx_seat_instances_flight_instance_cabin_id ON seat_instances (flight_instance_cabin_id);
CREATE INDEX idx_seat_instances_status ON seat_instances (status);
CREATE INDEX idx_seat_instances_booking_reference ON seat_instances (booking_reference);

-- Inbox-pattern dedup for the booking.confirmed/booking.cancelled consumers: a redelivered event
-- can never double-apply its seat-status side effect, even if it races a concurrent delivery.
CREATE TABLE IF NOT EXISTS processed_booking_events (
    id              CHAR(36) PRIMARY KEY,
    booking_id      CHAR(36) NOT NULL,
    event_type      VARCHAR(50) NOT NULL,
    processed_at    DATETIME(6),
    CONSTRAINT uq_processed_booking_events UNIQUE (booking_id, event_type)
);
