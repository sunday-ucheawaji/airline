package com.sunday.services.service;

import com.sunday.common_lib.enums.FlightStatus;
import com.sunday.common_lib.payload.request.FlightRequest;
import com.sunday.common_lib.payload.response.FlightResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface FlightService {

    FlightResponse createFlight(Long userId, FlightRequest request);
    List<FlightResponse> createFlights(Long userId, List<FlightRequest> requests);
    FlightResponse getFlightById(Long id);
    FlightResponse getFlightByNumber(String flightNumber);
    Page<FlightResponse> getFlightsByAirline(Long userId,
                                             Long airlineId,
                                             Long departureAirportId,
                                             Long arrivalAirportId,
                                             Pageable pageable);
    FlightResponse updateFlight(Long id, FlightRequest request, Long userId);
    FlightResponse changeStatus(Long id, FlightStatus status, Long userId);
    void deleteFlight(Long id, Long userId);

    Map<Long, FlightResponse> getFlightsByIds(List<Long> ids);
}
