package com.sunday.services.client;

import com.sunday.common_lib.exception.AirportException;
import com.sunday.common_lib.payload.response.AirportResponse;
import org.springframework.stereotype.Component;

@Component
public class LocationClientFallback implements LocationClient {

    @Override
    public AirportResponse getAirportById(Long id) throws AirportException {
        return null;
    }
}
