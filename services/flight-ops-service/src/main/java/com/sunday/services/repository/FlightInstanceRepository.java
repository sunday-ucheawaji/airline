package com.sunday.services.repository;

import java.util.UUID;
import com.sunday.services.model.FlightInstance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface FlightInstanceRepository extends JpaRepository<
        FlightInstance, UUID>,
        JpaSpecificationExecutor<FlightInstance> {

    @Query("SELECT fi FROM FlightInstance fi WHERE fi.airlineId = :airlineId" +
            " AND (:departureAirportId IS NULL OR fi.departureAirportId = :departureAirportId)" +
            " AND (:arrivalAirportId IS NULL OR fi.arrivalAirportId = :arrivalAirportId)" +
            " AND (:flightId IS NULL OR fi.flight.id = :flightId)" +
            " AND (:dayStart IS NULL OR fi.departureDateTime >= :dayStart)" +
            " AND (:dayEnd IS NULL OR fi.departureDateTime < :dayEnd)")
    Page<FlightInstance> findByAirlineIdWithFilters(
            @Param("airlineId") UUID airlineId,
            @Param("departureAirportId") UUID departureAirportId,
            @Param("arrivalAirportId") UUID arrivalAirportId,
            @Param("flightId") UUID flightId,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd,
            Pageable pageable);

    @Query("SELECT fi FROM FlightInstance fi JOIN FETCH fi.flight WHERE fi.id IN :ids")
    List<FlightInstance> findAllByIdInWithFlight(@Param("ids") Collection<UUID> ids);
}
