package com.sunday.services.service;

import java.util.UUID;
import com.sunday.common_lib.enums.FlightStatus;
import com.sunday.common_lib.payload.request.FlightRequest;
import com.sunday.common_lib.payload.response.FlightResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface FlightService {

    FlightResponse createFlight(UUID userId, FlightRequest request);
    List<FlightResponse> createFlights(UUID userId, List<FlightRequest> requests);
    FlightResponse getFlightById(UUID id);
    FlightResponse getFlightByNumber(String flightNumber);
    Page<FlightResponse> getFlightsByAirline(UUID userId,
                                             UUID airlineId,
                                             UUID departureAirportId,
                                             UUID arrivalAirportId,
                                             Pageable pageable);
    FlightResponse updateFlight(UUID id, FlightRequest request, UUID userId);
    FlightResponse changeStatus(UUID id, FlightStatus status, UUID userId);
    void deleteFlight(UUID id, UUID userId);

    Map<UUID, FlightResponse> getFlightsByIds(List<UUID> ids);
}
