package com.sunday.services.service;

import java.util.UUID;
import com.sunday.common_lib.payload.request.FlightScheduleRequest;
import com.sunday.common_lib.payload.response.FlightScheduleResponse;

import java.util.List;

public interface FlightScheduleService {

    FlightScheduleResponse createFlightSchedule(UUID userId, FlightScheduleRequest request);
    FlightScheduleResponse getFlightScheduleById(UUID id, UUID userId);

    List<FlightScheduleResponse> getFlightScheduleByAirline(UUID userId, UUID airlineId);

    FlightScheduleResponse updateFlightSchedule(UUID id, FlightScheduleRequest request, UUID userId);

    void deleteFlightSchedule(UUID id, UUID userId);
}
