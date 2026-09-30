package com.sunday.services.client;

import com.sunday.common_lib.payload.response.AirportResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class LocationClientFallback implements LocationClient {

    @Override
    public AirportResponse getAirportById(Long id) {
        return null;
    }

    @Override
    public Map<Long, AirportResponse> getAirportsByIds(List<Long> ids) {
        return Collections.emptyMap();
    }
}
