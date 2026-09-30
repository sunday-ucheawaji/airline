-- Formalizes the table Hibernate was previously auto-generating for the pre-existing
-- Flight entity, now that ddl-auto is `validate`. No new constraints introduced beyond
-- what the entity already declares: flight_number has no DB-level uniqueness (the
-- entity has no unique = true), only the app-level existsByFlightNumber() check.
-- aircraft_id moved to flight_instances (see V3) — a route/template has no aircraft of
-- its own; a specific dated departure does, independently reassignable.
CREATE TABLE IF NOT EXISTS flights (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    flight_number         VARCHAR(10) NOT NULL,
    airline_id            BIGINT NOT NULL,
    departure_airport_id  BIGINT NOT NULL,
    arrival_airport_id    BIGINT NOT NULL,
    status                ENUM('SCHEDULED','BOARDING','DEPARTED','IN_AIR','LANDED',
                                'ARRIVED','DELAYED','CANCELLED','DIVERTED','COMPLETED'),
    created_at            DATETIME(6) NOT NULL,
    updated_at            DATETIME(6) NOT NULL
);

CREATE INDEX idx_flights_flight_number ON flights (flight_number);
CREATE INDEX idx_flights_airline_id ON flights (airline_id);
CREATE INDEX idx_flights_status ON flights (status);
