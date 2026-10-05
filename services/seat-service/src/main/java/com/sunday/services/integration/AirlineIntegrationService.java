package com.sunday.services.integration;

import com.sunday.common_lib.payload.response.AircraftResponse;

import java.util.Collection;
import java.util.Map;

public interface AirlineIntegrationService {

    void requirePermission(Long userId, Long airlineId, String permission);

    void requirePermission(Long userId, Collection<Long> airlineIds, String permission);

    Long resolveAirlineIdForAircraft(Long aircraftId);

    Map<Long, Long> resolveAirlineIdsForAircrafts(Collection<Long> aircraftIds);

    AircraftResponse getAircraftByIdInternal(Long aircraftId);
}
