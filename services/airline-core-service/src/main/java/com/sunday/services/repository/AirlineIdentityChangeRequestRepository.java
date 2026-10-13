package com.sunday.services.repository;

import com.sunday.services.enums.IdentityChangeStatus;
import com.sunday.services.model.AirlineIdentityChangeRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AirlineIdentityChangeRequestRepository extends JpaRepository<AirlineIdentityChangeRequest, UUID> {

    boolean existsByAirlineIdAndStatus(UUID airlineId, IdentityChangeStatus status);

    List<AirlineIdentityChangeRequest> findByAirlineIdOrderByCreatedAtDesc(UUID airlineId);

    Optional<AirlineIdentityChangeRequest> findByIdAndAirlineId(UUID id, UUID airlineId);

    Page<AirlineIdentityChangeRequest> findByStatus(IdentityChangeStatus status, Pageable pageable);
}
