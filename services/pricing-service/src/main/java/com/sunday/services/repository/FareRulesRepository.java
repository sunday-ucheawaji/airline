package com.sunday.services.repository;

import com.sunday.services.model.FareRules;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FareRulesRepository extends JpaRepository<FareRules, Long> {

    Optional<FareRules> findByFareId(Long fareId);
    @EntityGraph(attributePaths = "fare")
    Page<FareRules> findByAirlineId(Long airlineId, Pageable pageable);
    boolean existsByFareId(Long fareId);
}
