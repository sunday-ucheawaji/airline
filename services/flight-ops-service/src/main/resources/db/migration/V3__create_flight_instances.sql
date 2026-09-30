-- Formalizes the table Hibernate was previously auto-generating for the pre-existing
-- FlightInstance entity, now that ddl-auto is `validate`.
CREATE TABLE IF NOT EXISTS flight_instances (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    airline_id               BIGINT,
    flight_id                BIGINT NOT NULL,
    aircraft_id              BIGINT,
    departure_airport_id     BIGINT NOT NULL,
    arrival_airport_id       BIGINT NOT NULL,
    schedule_id              BIGINT NOT NULL,
    departure_date_time      DATETIME NOT NULL,
    arrival_date_time        DATETIME NOT NULL,
    total_seats              INT NOT NULL,
    available_seats          INT NOT NULL,
    status                   ENUM('SCHEDULED','BOARDING','DEPARTED','IN_AIR','LANDED',
                                   'ARRIVED','DELAYED','CANCELLED','DIVERTED','COMPLETED') NOT NULL,
    min_advance_booking_days INT,
    max_advance_booking_days INT,
    is_active                BOOLEAN NOT NULL,
    terminal                 VARCHAR(255),
    gate                     VARCHAR(255),
    version                  BIGINT,
    CONSTRAINT uk_flight_instances_flight_departure UNIQUE (flight_id, departure_date_time),
    CONSTRAINT fk_flight_instances_flight FOREIGN KEY (flight_id) REFERENCES flights (id)
);

CREATE INDEX idx_flight_instances_airline_id ON flight_instances (airline_id);
CREATE INDEX idx_flight_instances_aircraft_id ON flight_instances (aircraft_id);
CREATE INDEX idx_flight_instances_status ON flight_instances (status);
CREATE INDEX idx_flight_instances_departure_date_time ON flight_instances (departure_date_time);
