-- meals.code was globally unique, stricter than the business rule (MealSpecification.hasCodeAndAirlineId
-- already only checks per-airline) — two airlines legitimately using the same meal code was rejected by
-- the database even though the application logic never meant to forbid it.
ALTER TABLE meals
    DROP INDEX uk_meals_code,
    ADD CONSTRAINT uk_meals_airline_code UNIQUE (airline_id, code);
