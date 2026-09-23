package com.sunday.services.client;

import com.sunday.common_lib.payload.response.AircraftResponse;
import com.sunday.common_lib.payload.response.AirlineResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AirlineClientFallback implements AirlineClient {

    @Override
    public List<AirlineResponse> getMyAirlines(Long userId) {
        return null;
    }

    @Override
    public List<String> getMyPermissions(Long airlineId, Long userId) {
        return null;
    }

    @Override
    public void requirePermission(List<Long> airlineIds, String permission, Long userId) {
        // A void method can't signal "unavailable" via a null return — throw directly instead.
        throw new RuntimeException("Airline service unavailable");
    }

    @Override
    public AircraftResponse getAircraftById(Long id) {
        return null;
    }
}
