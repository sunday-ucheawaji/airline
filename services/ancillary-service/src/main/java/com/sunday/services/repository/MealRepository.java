package com.sunday.services.repository;

import java.util.UUID;
import com.sunday.services.model.Meal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface MealRepository extends JpaRepository<Meal, UUID>, JpaSpecificationExecutor<Meal> {

    Optional<Meal> findByCode(String code);

    /** Batch duplicate-check for bulk create: candidates whose (airlineId, code) might already exist. */
    List<Meal> findByAirlineIdInAndCodeIn(List<UUID> airlineIds, List<String> codes);
}
