package com.sunday.services.Integration;

import com.sunday.common_lib.payload.response.AircraftResponse;

public interface AirlineIntegrationService {
    Long getAirlineIdForUser(Long userId);
    AircraftResponse getAircraftById(Long aircraftId);
}
