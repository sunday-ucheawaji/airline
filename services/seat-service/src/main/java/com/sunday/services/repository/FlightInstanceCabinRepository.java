package com.sunday.services.repository;

import java.util.UUID;
import com.sunday.services.model.FlightInstanceCabin;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FlightInstanceCabinRepository extends JpaRepository<FlightInstanceCabin, UUID> {

    Page<FlightInstanceCabin> findByFlightInstanceId(UUID flightInstanceId, Pageable pageable);

    List<FlightInstanceCabin> findAllByFlightInstanceId(UUID flightInstanceId);

    List<FlightInstanceCabin> findAllByFlightInstanceIdIn(List<UUID> flightInstanceIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT fc FROM FlightInstanceCabin fc WHERE fc.id = :id")
    Optional<FlightInstanceCabin> findByIdForUpdate(@Param("id") UUID id);

    FlightInstanceCabin findByFlightInstanceIdAndCabinClassId(UUID flightInstanceId, UUID cabinClassId);
}
