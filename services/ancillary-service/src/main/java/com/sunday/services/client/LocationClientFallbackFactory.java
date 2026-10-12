package com.sunday.services.client;

import com.sunday.common_lib.payload.response.AirportResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class LocationClientFallbackFactory implements FallbackFactory<LocationClient> {

    @Override
    public LocationClient create(Throwable cause) {
        return id -> {
            log.warn("location-service call failed – airport {} enrichment returns null: {}", id, cause.getMessage(), cause);
            return null;
        };
    }
}
