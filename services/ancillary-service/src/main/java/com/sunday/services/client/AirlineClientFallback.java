package com.sunday.services.client;

import java.util.UUID;
import com.sunday.common_lib.payload.response.AircraftResponse;
import com.sunday.common_lib.payload.response.AirlineResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AirlineClientFallback implements AirlineClient {

    @Override
    public List<AirlineResponse> getMyAirlines(UUID userId) {
        return null;
    }

    @Override
    public List<String> getMyPermissions(UUID airlineId, UUID userId) {
        return null;
    }

    @Override
    public void requirePermission(List<UUID> airlineIds, String permission, UUID userId) {
        // A void method can't signal "unavailable" via a null return — throw directly instead.
        throw new RuntimeException("Airline service unavailable");
    }

    @Override
    public AircraftResponse getAircraftById(UUID id) {
        return null;
    }
}
