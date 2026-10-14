ALTER TABLE flight_cabin_ancillaries
    ADD CONSTRAINT uk_flight_cabin_ancillary UNIQUE (flight_id, cabin_class, ancillary_id);
