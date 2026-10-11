package com.sunday.services.repository;

import java.util.UUID;
import com.sunday.services.model.FareRules;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FareRulesRepository extends JpaRepository<FareRules, UUID> {

    Optional<FareRules> findByFareId(UUID fareId);
    @EntityGraph(attributePaths = "fare")
    Page<FareRules> findByAirlineId(UUID airlineId, Pageable pageable);
    boolean existsByFareId(UUID fareId);
}
