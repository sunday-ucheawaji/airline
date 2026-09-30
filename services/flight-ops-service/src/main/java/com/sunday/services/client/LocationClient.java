package com.sunday.services.client;

import com.sunday.common_lib.payload.response.AirportResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "location-service", fallback = LocationClientFallback.class)
public interface LocationClient {

    @GetMapping("/api/airports/{id}")
    AirportResponse getAirportById(@PathVariable Long id);

    /** Batch variant of {@link #getAirportById}: one round trip instead of one per airport. */
    @PostMapping("/api/airports/batch")
    Map<Long, AirportResponse> getAirportsByIds(@RequestBody List<Long> ids);
}
