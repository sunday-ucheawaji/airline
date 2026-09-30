package com.sunday.services.service;

import com.sunday.common_lib.payload.request.FlightScheduleRequest;
import com.sunday.common_lib.payload.response.FlightScheduleResponse;

import java.util.List;

public interface FlightScheduleService {

    FlightScheduleResponse createFlightSchedule(Long userId, FlightScheduleRequest request);
    FlightScheduleResponse getFlightScheduleById(Long id, Long userId);

    List<FlightScheduleResponse> getFlightScheduleByAirline(Long userId, Long airlineId);

    FlightScheduleResponse updateFlightSchedule(Long id, FlightScheduleRequest request, Long userId);

    void deleteFlightSchedule(Long id, Long userId);
}
