CREATE TABLE IF NOT EXISTS cities (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    city_code    VARCHAR(10)  NOT NULL,
    country_code VARCHAR(5)   NOT NULL,
    country_name VARCHAR(100) NOT NULL,
    region_code  VARCHAR(10),
    time_zone_id VARCHAR(50),
    CONSTRAINT uk_cities_city_code UNIQUE (city_code),
    INDEX idx_city_name (name),
    INDEX idx_country_code (country_code)
);
