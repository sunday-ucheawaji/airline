CREATE TABLE IF NOT EXISTS ancillaries (
    id            CHAR(36) PRIMARY KEY,
    type          ENUM('BAGGAGE', 'TRAVEL_PROTECTION') NOT NULL,
    sub_type      VARCHAR(100),
    rfisc         VARCHAR(10),
    name          VARCHAR(200) NOT NULL,
    description   VARCHAR(1000),
    metadata      TEXT,
    display_order INT,
    airline_id    CHAR(36) NOT NULL
);
