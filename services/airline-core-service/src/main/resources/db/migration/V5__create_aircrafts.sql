-- Aircraft/AircraftController were untouched by the onboarding/membership redesign
-- (out of scope) — this migration only formalizes the table Hibernate was previously
-- auto-generating for the pre-existing Aircraft entity, now that ddl-auto is `validate`.
CREATE TABLE IF NOT EXISTS aircrafts (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    aircraft_code            VARCHAR(20) NOT NULL,
    model                    VARCHAR(50) NOT NULL,
    manufacturer             VARCHAR(50) NOT NULL,
    seating_capacity         INT NOT NULL,
    economy_seats            INT,
    premium_economy_seats    INT,
    business_seats           INT,
    first_class_seats        INT,
    range_km                 INT,
    cruising_speed_kmh       INT,
    max_altitude_ft          INT,
    year_of_manufacture      INT,
    registration_date        DATE,
    next_maintenance_date    DATE,
    status                   ENUM('ACTIVE', 'MAINTENANCE', 'INACTIVE', 'RETIRED') NOT NULL DEFAULT 'ACTIVE',
    is_available              BOOLEAN NOT NULL DEFAULT TRUE,
    airline_id               BIGINT NOT NULL,
    current_airport_id       BIGINT,
    created_at                DATETIME(6) NOT NULL,
    updated_at                DATETIME(6) NOT NULL,
    CONSTRAINT uk_aircrafts_code UNIQUE (aircraft_code),
    CONSTRAINT fk_aircrafts_airline FOREIGN KEY (airline_id) REFERENCES airlines (id)
);

CREATE INDEX idx_aircraft_code ON aircrafts (aircraft_code);
CREATE INDEX idx_aircraft_model ON aircrafts (model);
CREATE INDEX idx_aircraft_airline ON aircrafts (airline_id);
