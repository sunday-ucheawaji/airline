package com.sunday.services.repository;

import java.util.UUID;
import com.sunday.services.model.Flight;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface FlightRepository extends JpaRepository<Flight, UUID> {

    Optional<Flight> findByFlightNumber(String flightNumber);
    boolean existsByFlightNumber(String flightNumber);
    boolean existsByFlightNumberAndIdNot(String flightNumber, UUID id);

    @Query("SELECT f.flightNumber FROM Flight f WHERE f.flightNumber IN :numbers")
    Set<String> findExistingFlightNumbers(@Param("numbers") Collection<String> numbers);

    @Query("""
            SELECT f FROM Flight f
            WHERE f.airlineId = :airlineId
              AND (:depId IS NULL OR f.departureAirportId = :depId)
              AND (:arrId IS NULL OR f.arrivalAirportId = :arrId)
            """)
    Page<Flight> findByAirlineIdAndOptionalRoute(
            @Param("airlineId") UUID airlineId,
            @Param("depId") UUID departureAirportId,
            @Param("arrId") UUID arrivalAirportId,
            Pageable pageable);
}
