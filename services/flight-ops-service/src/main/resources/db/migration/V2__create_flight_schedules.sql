-- Formalizes the tables Hibernate was previously auto-generating for the pre-existing
-- FlightSchedule entity (including its @ElementCollection operatingDays child table),
-- now that ddl-auto is `validate`.
CREATE TABLE IF NOT EXISTS flight_schedules (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    flight_id             BIGINT NOT NULL,
    departure_airport_id  BIGINT NOT NULL,
    arrival_airport_id    BIGINT NOT NULL,
    departure_time        TIME NOT NULL,
    arrival_time          TIME NOT NULL,
    start_date            DATE NOT NULL,
    end_date              DATE NOT NULL,
    recurrence_type       ENUM('DAILY','WEEKLY','CUSTOM','NONE'),
    is_active             BOOLEAN NOT NULL,
    version               BIGINT,
    CONSTRAINT fk_flight_schedules_flight FOREIGN KEY (flight_id) REFERENCES flights (id)
);

CREATE TABLE IF NOT EXISTS schedule_operating_days (
    schedule_id  BIGINT NOT NULL,
    day_of_week  ENUM('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'),
    CONSTRAINT fk_schedule_operating_days_schedule FOREIGN KEY (schedule_id) REFERENCES flight_schedules (id)
);

CREATE INDEX idx_flight_schedules_flight_id ON flight_schedules (flight_id);
CREATE INDEX idx_flight_schedules_departure_airport_id ON flight_schedules (departure_airport_id);
