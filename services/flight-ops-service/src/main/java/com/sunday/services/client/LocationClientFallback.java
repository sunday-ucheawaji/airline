package com.sunday.services.client;

import java.util.UUID;
import com.sunday.common_lib.payload.response.AirportResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class LocationClientFallback implements LocationClient {

    @Override
    public AirportResponse getAirportById(UUID id) {
        return null;
    }

    @Override
    public Map<UUID, AirportResponse> getAirportsByIds(List<UUID> ids) {
        return Collections.emptyMap();
    }
}
