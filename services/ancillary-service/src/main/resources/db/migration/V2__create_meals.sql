CREATE TABLE IF NOT EXISTS meals (
    id                       CHAR(36) PRIMARY KEY,
    code                     VARCHAR(10) NOT NULL,
    name                     VARCHAR(200) NOT NULL,
    meal_type                VARCHAR(50) NOT NULL,
    dietary_restriction      VARCHAR(100),
    ingredients              VARCHAR(2000),
    image_url                VARCHAR(500),
    available                BIT(1) NOT NULL,
    requires_advance_booking BIT(1) NOT NULL,
    advance_booking_hours    INT,
    display_order            INT,
    airline_id               CHAR(36) NOT NULL,
    created_at               DATETIME(6) NOT NULL,
    updated_at               DATETIME(6) NOT NULL,
    CONSTRAINT uk_meals_code UNIQUE (code),
    INDEX idx_meal_airline (airline_id),
    INDEX idx_meal_code (code)
);
