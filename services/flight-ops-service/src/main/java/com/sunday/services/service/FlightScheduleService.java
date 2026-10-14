package com.sunday.services.service;

import java.util.UUID;
import com.sunday.common_lib.payload.request.FlightScheduleRequest;
import com.sunday.common_lib.payload.response.FlightScheduleResponse;
import com.sunday.common_lib.payload.response.ScheduleChangeResponse;

import java.util.List;

public interface FlightScheduleService {

    FlightScheduleResponse createFlightSchedule(UUID userId, FlightScheduleRequest request);
    FlightScheduleResponse getFlightScheduleById(UUID id, UUID userId);

    List<FlightScheduleResponse> getFlightScheduleByAirline(UUID userId, UUID airlineId);

    /** Only future departures without sales follow the change; the rest are kept and counted. */
    ScheduleChangeResponse updateFlightSchedule(UUID id, FlightScheduleRequest request, UUID userId);

    /** Removes future departures without sales; the rest stay, detached from the schedule. */
    ScheduleChangeResponse deleteFlightSchedule(UUID id, UUID userId);
}
