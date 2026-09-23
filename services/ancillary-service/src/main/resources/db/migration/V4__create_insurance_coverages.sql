CREATE TABLE IF NOT EXISTS insurance_coverages (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    ancillary_id      BIGINT NOT NULL,
    coverage_type     ENUM(
                          'BAGGAGE_LOSS', 'BAGGAGE_DELAY', 'BAGGAGE_ASSISTANCE',
                          'PERSONAL_ACCIDENT',
                          'TRIP_DELAY', 'TRIP_CANCELLATION', 'MISSED_CONNECTION', 'DIVERTED_FLIGHT',
                          'FREE_DATE_CHANGE', 'ZERO_CANCELLATION',
                          'EMERGENCY_ASSISTANCE', 'TRAVEL_DOCUMENT_LOSS', 'MEDICAL_EMERGENCY'
                      ) NOT NULL,
    name              VARCHAR(200) NOT NULL,
    description       VARCHAR(1000),
    coverage_amount   DOUBLE NOT NULL,
    currency          VARCHAR(3),
    is_flat           BIT(1) NOT NULL,
    claim_condition   VARCHAR(500),
    emergency_contact VARCHAR(100),
    display_order     INT,
    active            BIT(1) NOT NULL,
    CONSTRAINT fk_insurance_coverages_ancillary FOREIGN KEY (ancillary_id) REFERENCES ancillaries (id)
);
