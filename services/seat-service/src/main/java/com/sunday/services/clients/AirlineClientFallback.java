package com.sunday.services.clients;

import com.sunday.common_lib.payload.response.AircraftResponse;
import com.sunday.common_lib.payload.response.AirlineResponse;
import org.springframework.stereotype.Component;

@Component
public class AirlineClientFallback implements AirlineClient {

    @Override
    public AirlineResponse getAirlineByOwner(Long userId) {
        return null;
    }

    @Override
    public AircraftResponse getAircraftById(Long id) {
        return null;
    }
}
