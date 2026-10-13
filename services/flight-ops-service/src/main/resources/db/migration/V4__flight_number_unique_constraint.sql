-- flight_number was only enforced by an app-level check-then-insert (existsByFlightNumber), which
-- races under concurrent creates. The application already treats flight numbers as globally unique
-- (not per-airline), so this matches the existing business rule rather than introducing a new one.
ALTER TABLE flights
    ADD CONSTRAINT uk_flights_flight_number UNIQUE (flight_number);
