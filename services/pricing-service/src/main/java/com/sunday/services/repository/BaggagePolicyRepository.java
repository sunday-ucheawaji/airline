package com.sunday.services.repository;

import java.util.UUID;
import com.sunday.services.model.BaggagePolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface BaggagePolicyRepository extends JpaRepository<BaggagePolicy, UUID> {

    Optional<BaggagePolicy> findByFareId(UUID fareId);
    @EntityGraph(attributePaths = "fare")
    Page<BaggagePolicy> findByAirlineId(UUID airlineId, Pageable pageable);
    boolean existsByFareId(UUID fareId);

    @Query("SELECT b.fare.id FROM BaggagePolicy b WHERE b.fare.id IN :fareIds")
    Set<UUID> findFareIdsWithExistingPolicy(@Param("fareIds") Collection<UUID> fareIds);
}
