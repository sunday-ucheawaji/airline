package com.sunday.services.service.impl;

import com.sunday.common_lib.constants.FlightOpsPermissions;
import com.sunday.common_lib.enums.FlightStatus;
import com.sunday.common_lib.payload.request.FlightInstanceRequest;
import com.sunday.common_lib.payload.request.FlightScheduleRequest;
import com.sunday.common_lib.payload.response.AircraftResponse;
import com.sunday.common_lib.payload.response.AirportResponse;
import com.sunday.common_lib.payload.response.FlightScheduleResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.Integration.AirlineIntegrationService;
import com.sunday.services.client.LocationClient;
import com.sunday.services.mapper.FlightScheduleMapper;
import com.sunday.services.model.Flight;
import com.sunday.services.model.FlightSchedule;
import com.sunday.services.repository.FlightRepository;
import com.sunday.services.repository.FlightScheduleRepository;
import com.sunday.services.service.FlightInstanceService;
import com.sunday.services.service.FlightScheduleService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FlightScheduleServiceImpl implements FlightScheduleService {

    private final FlightScheduleRepository flightScheduleRepository;
    private final FlightRepository flightRepository;
    private final FlightInstanceService flightInstanceService;
    private final AirlineIntegrationService airlineIntegrationService;
    private final LocationClient locationClient;

    @Override
    @Transactional
    public FlightScheduleResponse createFlightSchedule(Long userId, FlightScheduleRequest request) {
        Flight flight = flightRepository.findById(request.getFlightId())
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format(ErrorMessageUtil.FLIGHT_NOT_FOUND_BY_ID, request.getFlightId())));
        airlineIntegrationService.requirePermission(userId, flight.getAirlineId(), FlightOpsPermissions.SCHEDULE_MANAGE);

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException(ErrorMessageUtil.SCHEDULE_END_DATE_BEFORE_START_DATE);
        }

        FlightSchedule schedule = FlightScheduleMapper.toEntity(request, flight);
        FlightSchedule savedSchedule = flightScheduleRepository.save(schedule);

        // Resolved once — loop-invariant, reused for every generated instance instead of being
        // re-fetched (and the permission re-checked) on every iteration.
        AircraftResponse aircraft = airlineIntegrationService.getAircraftById(request.getAircraftId(), userId);

        Set<DayOfWeek> operatingDays = new HashSet<>(schedule.getOperatingDays()); // e.g., MON, WED, FRI
        LocalDate startDate = schedule.getStartDate();
        LocalDate endDate = schedule.getEndDate();

        List<FlightInstanceRequest> instanceRequests = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (operatingDays.contains(date.getDayOfWeek())) {
                instanceRequests.add(FlightInstanceRequest.builder()
                        .scheduleId(savedSchedule.getId())
                        .flightId(flight.getId())
                        .aircraftId(request.getAircraftId())
                        .arrivalAirportId(flight.getArrivalAirportId())
                        .departureAirportId(flight.getDepartureAirportId())
                        .departureDateTime(LocalDateTime.of(date, schedule.getDepartureTime()))
                        .arrivalDateTime(LocalDateTime.of(date, schedule.getArrivalTime()))
                        .totalSeats(aircraft.getTotalSeats())
                        .status(FlightStatus.SCHEDULED)
                        .build());
            }
        }

        flightInstanceService.createFlightInstancesInBulk(flight, aircraft, instanceRequests);

        return getFlightScheduleResponse(savedSchedule);
    }

    @Override
    @Transactional(readOnly = true)
    public FlightScheduleResponse getFlightScheduleById(Long id, Long userId) {
        FlightSchedule schedule = flightScheduleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format(ErrorMessageUtil.FLIGHT_SCHEDULE_NOT_FOUND_BY_ID, id)));
        airlineIntegrationService.requirePermission(userId, schedule.getFlight().getAirlineId(), FlightOpsPermissions.SCHEDULE_READ);
        return getFlightScheduleResponse(schedule);
    }

    @Override
    public List<FlightScheduleResponse> getFlightScheduleByAirline(Long userId, Long airlineId) {
        airlineIntegrationService.requirePermission(userId, airlineId, FlightOpsPermissions.SCHEDULE_READ);
        List<FlightSchedule> schedules = flightScheduleRepository.findByFlightAirlineId(airlineId);
        return schedules
                .stream()
                .map(this::getFlightScheduleResponse)
                .collect(Collectors.toList());
    }

    @Override
    public FlightScheduleResponse updateFlightSchedule(Long id, FlightScheduleRequest request, Long userId) {
        FlightSchedule existing = flightScheduleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format(ErrorMessageUtil.FLIGHT_SCHEDULE_NOT_FOUND_BY_ID, id)));
        airlineIntegrationService.requirePermission(userId, existing.getFlight().getAirlineId(), FlightOpsPermissions.SCHEDULE_MANAGE);

        FlightScheduleMapper.updateEntity(request, existing);
        FlightSchedule saved = flightScheduleRepository.save(existing);
        return getFlightScheduleResponse(saved);
    }

    @Override
    public void deleteFlightSchedule(Long id, Long userId) {
        FlightSchedule schedule = flightScheduleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format(ErrorMessageUtil.FLIGHT_SCHEDULE_NOT_FOUND_BY_ID, id)));
        airlineIntegrationService.requirePermission(userId, schedule.getFlight().getAirlineId(), FlightOpsPermissions.SCHEDULE_MANAGE);
        flightScheduleRepository.delete(schedule);
    }

    public FlightScheduleResponse getFlightScheduleResponse(
            FlightSchedule schedule) {
        AirportResponse arrivalAirport = locationClient.getAirportById(schedule.getArrivalAirportId());
        AirportResponse departureAirport = locationClient.getAirportById(schedule.getDepartureAirportId());
        return FlightScheduleMapper.toResponse(schedule, arrivalAirport, departureAirport);
    }
}
