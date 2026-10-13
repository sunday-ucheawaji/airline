package com.sunday.services.client;

import com.sunday.common_lib.payload.response.FlightInstanceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/** Open read of a flight instance, to check which aircraft operates it before inventory is attached to it. */
@FeignClient(name = "flight-ops-service", fallbackFactory = FlightOpsClientFallbackFactory.class)
public interface FlightOpsClient {

    @GetMapping("/api/flight-instances/{id}")
    FlightInstanceResponse getFlightInstance(@PathVariable UUID id);
}
