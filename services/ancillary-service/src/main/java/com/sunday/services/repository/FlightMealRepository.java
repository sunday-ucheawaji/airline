package com.sunday.services.repository;

import com.sunday.services.model.FlightMeal;
import com.sunday.services.model.Meal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface FlightMealRepository extends JpaRepository<FlightMeal, Long>, JpaSpecificationExecutor<FlightMeal> {

    Optional<FlightMeal> findByFlightIdAndMeal(Long flightId, Meal meal);

    void deleteByFlightId(Long flightId);

    /** Batch duplicate-check for bulk create: candidates whose (flightId, meal.id) might already exist. */
    List<FlightMeal> findByFlightIdInAndMeal_IdIn(List<Long> flightIds, List<Long> mealIds);
}
