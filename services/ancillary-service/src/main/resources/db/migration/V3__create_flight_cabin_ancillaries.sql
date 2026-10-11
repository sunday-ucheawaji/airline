CREATE TABLE IF NOT EXISTS flight_cabin_ancillaries (
    id               CHAR(36) PRIMARY KEY,
    flight_id        CHAR(36) NOT NULL,
    cabin_class_id   CHAR(36) NOT NULL,
    ancillary_id     CHAR(36) NOT NULL,
    available        BIT(1) NOT NULL,
    max_quantity     INT,
    price            DECIMAL(12,2),
    currency         VARCHAR(255),
    included_in_fare BIT(1) NOT NULL,
    CONSTRAINT fk_flight_cabin_ancillaries_ancillary FOREIGN KEY (ancillary_id) REFERENCES ancillaries (id)
);
