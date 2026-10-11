package com.sunday.services.integration;

import java.util.UUID;
import com.sunday.common_lib.payload.response.AircraftResponse;

import java.util.Collection;
import java.util.Map;

public interface AirlineIntegrationService {

    void requirePermission(UUID userId, UUID airlineId, String permission);

    void requirePermission(UUID userId, Collection<UUID> airlineIds, String permission);

    UUID resolveAirlineIdForAircraft(UUID aircraftId);

    Map<UUID, UUID> resolveAirlineIdsForAircrafts(Collection<UUID> aircraftIds);

    AircraftResponse getAircraftByIdInternal(UUID aircraftId);
}
