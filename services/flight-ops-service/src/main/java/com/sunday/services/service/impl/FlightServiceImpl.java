package com.sunday.services.service.impl;

import com.sunday.common_lib.constants.FlightOpsPermissions;
import com.sunday.common_lib.enums.FlightStatus;
import com.sunday.common_lib.exception.BadRequestException;
import com.sunday.common_lib.payload.request.FlightRequest;
import com.sunday.common_lib.payload.response.AirlineResponse;
import com.sunday.common_lib.payload.response.AirportResponse;
import com.sunday.common_lib.payload.response.FlightResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.Integration.AirlineIntegrationService;
import com.sunday.services.client.AirlineClient;
import com.sunday.services.client.LocationClient;
import com.sunday.services.mapper.FlightMapper;
import com.sunday.services.model.Flight;
import com.sunday.services.repository.FlightRepository;
import com.sunday.services.service.FlightService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
public class FlightServiceImpl implements FlightService {

    private final FlightRepository flightRepository;
    private final AirlineClient airlineClient;
    private final LocationClient locationClient;
    private final AirlineIntegrationService airlineIntegrationService;

    @Override
    public FlightResponse createFlight(Long userId, FlightRequest request) {
        if (flightRepository.existsByFlightNumber(request.getFlightNumber())) {
            throw new IllegalArgumentException(
                    String.format(ErrorMessageUtil.FLIGHT_NUMBER_ALREADY_EXISTS, request.getFlightNumber()));
        }
        Long airlineId = request.getAirlineId();
        if (airlineId == null) {
            throw new BadRequestException(ErrorMessageUtil.AIRLINE_ID_REQUIRED);
        }
        airlineIntegrationService.requirePermission(userId, airlineId, FlightOpsPermissions.FLIGHT_MANAGE);

        Flight flight = FlightMapper.toEntity(request);
        Flight saved = flightRepository.save(flight);
        return getFlightResponse(saved);
    }

    @Override
    public List<FlightResponse> createFlights(Long userId, List<FlightRequest> requests) {
        if (requests.stream().anyMatch(req -> req.getAirlineId() == null)) {
            throw new BadRequestException(ErrorMessageUtil.AIRLINE_ID_REQUIRED);
        }
        Set<Long> airlineIds = requests.stream().map(FlightRequest::getAirlineId).collect(Collectors.toSet());
        airlineIntegrationService.requirePermission(userId, airlineIds, FlightOpsPermissions.FLIGHT_MANAGE);

        // Single DB call to find all already-existing flight numbers
        Set<String> existingNumbers = flightRepository.findExistingFlightNumbers(
                requests.stream().map(FlightRequest::getFlightNumber).collect(Collectors.toList()));

        List<Flight> toSave = requests.stream()
                .filter(req -> !existingNumbers.contains(req.getFlightNumber()))
                .map(FlightMapper::toEntity)
                .collect(Collectors.toList());

        List<Flight> saved = flightRepository.saveAll(toSave);

        // One round trip per external service instead of one per distinct id.
        List<Long> distinctAirlineIds = saved.stream().map(Flight::getAirlineId).distinct().toList();
        List<Long> distinctAirportIds = saved.stream()
                .flatMap(f -> Stream.of(f.getDepartureAirportId(), f.getArrivalAirportId()))
                .distinct().toList();
        Map<Long, AirlineResponse> airlines = airlineClient.getAirlinesByIdsInternal(distinctAirlineIds);
        Map<Long, AirportResponse> airports = locationClient.getAirportsByIds(distinctAirportIds);

        List<FlightResponse> responses = new ArrayList<>();
        for (Flight flight : saved) {
            responses.add(FlightMapper.toResponse(
                    flight,
                    airlines.get(flight.getAirlineId()),
                    airports.get(flight.getDepartureAirportId()),
                    airports.get(flight.getArrivalAirportId())));
        }
        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public FlightResponse getFlightById(Long id)  {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format(ErrorMessageUtil.FLIGHT_NOT_FOUND_BY_ID, id)));

        return getFlightResponse(flight);
    }

    @Override
    @Transactional(readOnly = true)
    public FlightResponse getFlightByNumber(String flightNumber) {
        Flight flight = flightRepository.findByFlightNumber(flightNumber)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format(ErrorMessageUtil.FLIGHT_NOT_FOUND_BY_NUMBER, flightNumber)));
        return getFlightResponse(flight);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FlightResponse> getFlightsByAirline(Long userId, Long airlineId, Long departureAirportId, Long arrivalAirportId, Pageable pageable) {
        airlineIntegrationService.requirePermission(userId, airlineId, FlightOpsPermissions.FLIGHT_READ);
        return flightRepository.findByAirlineIdAndOptionalRoute(
                        airlineId,
                        departureAirportId,
                        arrivalAirportId,
                        pageable
                )
                .map(this::getFlightResponse);
    }

    @Override
    public FlightResponse updateFlight(Long id, FlightRequest request, Long userId) {
        Flight existing = flightRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(String.format(ErrorMessageUtil.FLIGHT_NOT_FOUND_BY_ID, id)));
        airlineIntegrationService.requirePermission(userId, existing.getAirlineId(), FlightOpsPermissions.FLIGHT_MANAGE);

        if (request.getFlightNumber() != null &&
                flightRepository.existsByFlightNumberAndIdNot(request.getFlightNumber(), id)) {
            throw new IllegalArgumentException(
                    String.format(ErrorMessageUtil.FLIGHT_NUMBER_ALREADY_EXISTS, request.getFlightNumber()));
        }

        FlightMapper.updateEntity(request, existing);
        Flight saved = flightRepository.save(existing);
        return getFlightResponse(saved);
    }

    @Override
    public FlightResponse changeStatus(Long id, FlightStatus status, Long userId) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(String.format(ErrorMessageUtil.FLIGHT_NOT_FOUND_BY_ID, id)));
        airlineIntegrationService.requirePermission(userId, flight.getAirlineId(), FlightOpsPermissions.FLIGHT_MANAGE);
        flight.setStatus(status);
        return getFlightResponse(flight);
    }

    @Override
    public void deleteFlight(Long id, Long userId) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(String.format(ErrorMessageUtil.FLIGHT_NOT_FOUND_BY_ID, id)));
        airlineIntegrationService.requirePermission(userId, flight.getAirlineId(), FlightOpsPermissions.FLIGHT_MANAGE);
        flightRepository.delete(flight);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, FlightResponse> getFlightsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        List<Flight> flights = flightRepository.findAllById(ids);

        // One round trip per external service instead of one per distinct id.
        List<Long> distinctAirlineIds = flights.stream().map(Flight::getAirlineId).distinct().toList();
        List<Long> distinctAirportIds = flights.stream()
                .flatMap(f -> Stream.of(f.getDepartureAirportId(), f.getArrivalAirportId()))
                .distinct().toList();
        Map<Long, AirlineResponse> airlines = airlineClient.getAirlinesByIdsInternal(distinctAirlineIds);
        Map<Long, AirportResponse> airports = locationClient.getAirportsByIds(distinctAirportIds);

        Map<Long, FlightResponse> result = new HashMap<>();
        for (Flight flight : flights) {
            result.put(flight.getId(), FlightMapper.toResponse(
                    flight,
                    airlines.get(flight.getAirlineId()),
                    airports.get(flight.getDepartureAirportId()),
                    airports.get(flight.getArrivalAirportId())));
        }
        return result;
    }

    private FlightResponse getFlightResponse(Flight flight) {
        AirlineResponse airline = airlineClient.getAirlineByIdInternal(flight.getAirlineId());
        AirportResponse departureAirport = locationClient.getAirportById(flight.getDepartureAirportId());
        AirportResponse arrivalAirport = locationClient.getAirportById(flight.getArrivalAirportId());
        return FlightMapper.toResponse(flight, airline, departureAirport, arrivalAirport);
    }
}
