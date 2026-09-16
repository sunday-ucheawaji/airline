package com.sunday.services.service;

import com.sunday.common_lib.exception.AirportException;
import com.sunday.common_lib.payload.request.FlightScheduleRequest;
import com.sunday.common_lib.payload.response.FlightScheduleResponse;

import java.util.List;

public interface FlightScheduleService {

    FlightScheduleResponse createFlightSchedule(Long userId, FlightScheduleRequest request) throws Exception;
    FlightScheduleResponse getFlightScheduleById(Long id) throws AirportException;

    List<FlightScheduleResponse> getFlightScheduleByAirline(Long userId);

    FlightScheduleResponse updateFlightSchedule(Long id, FlightScheduleRequest request) throws AirportException;

    void deleteFlightSchedule(Long id);
}
