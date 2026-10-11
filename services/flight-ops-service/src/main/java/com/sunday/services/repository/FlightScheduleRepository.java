package com.sunday.services.repository;

import java.util.UUID;
import com.sunday.services.model.FlightSchedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface FlightScheduleRepository extends JpaRepository<FlightSchedule, UUID> {

    List<FlightSchedule> findByFlightId(UUID flightId);
    Page<FlightSchedule> findByFlightId(UUID flightId, Pageable pageable);

    List<FlightSchedule> findByFlightAirlineId(UUID airlineId);

    @Query("SELECT fs FROM FlightSchedule fs WHERE fs.flight.id = :flightId AND fs.isActive = true AND fs.startDate <= :date AND fs.endDate >= :date")
    List<FlightSchedule> findActiveSchedulesForDate(
            @Param("flightId") UUID flightId,
            @Param("date") LocalDate date);

    @Query("SELECT fs FROM FlightSchedule fs WHERE fs.departureAirportId = :depId AND fs.arrivalAirportId = :arrId AND fs.isActive = true")
    List<FlightSchedule> findByRoute(
            @Param("depId") UUID departureAirportId,
            @Param("arrId") UUID arrivalAirportId);

    Page<FlightSchedule> findByDepartureAirportId(UUID departureAirportId, Pageable pageable);
}
