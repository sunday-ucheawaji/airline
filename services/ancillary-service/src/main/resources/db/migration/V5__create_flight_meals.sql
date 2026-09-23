CREATE TABLE IF NOT EXISTS flight_meals (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    flight_id     BIGINT NOT NULL,
    meal_id       BIGINT NOT NULL,
    available     BIT(1) NOT NULL,
    price         DOUBLE,
    display_order INT,
    CONSTRAINT uk_flight_meal UNIQUE (flight_id, meal_id),
    CONSTRAINT fk_flight_meals_meal FOREIGN KEY (meal_id) REFERENCES meals (id),
    INDEX idx_flight_meal_flight (flight_id),
    INDEX idx_flight_meal_meal (meal_id)
);
