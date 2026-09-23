CREATE TABLE IF NOT EXISTS flight_cabin_ancillaries (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    flight_id        BIGINT NOT NULL,
    cabin_class_id   BIGINT NOT NULL,
    ancillary_id     BIGINT NOT NULL,
    available        BIT(1) NOT NULL,
    max_quantity     INT,
    price            DOUBLE,
    currency         VARCHAR(255),
    included_in_fare BIT(1) NOT NULL,
    CONSTRAINT fk_flight_cabin_ancillaries_ancillary FOREIGN KEY (ancillary_id) REFERENCES ancillaries (id)
);
