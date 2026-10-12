package com.sunday.services.client;

import java.util.UUID;
import com.sunday.common_lib.payload.response.AircraftResponse;
import com.sunday.common_lib.payload.response.AirlineResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "airline-core-service", fallbackFactory = AirlineClientFallbackFactory.class)
public interface AirlineClient {

    @GetMapping("/api/airlines/mine")
    List<AirlineResponse> getMyAirlines(@RequestHeader("X-User-Id") UUID userId);

    @GetMapping("/api/airlines/{airlineId}/permissions")
    List<String> getMyPermissions(@PathVariable UUID airlineId, @RequestHeader("X-User-Id") UUID userId);

    @GetMapping("/api/airlines/permissions/check")
    void requirePermission(
            @RequestParam List<UUID> airlineIds,
            @RequestParam String permission,
            @RequestHeader("X-User-Id") UUID userId);

    @GetMapping("/api/aircrafts/{id}")
    AircraftResponse getAircraftById(@PathVariable("id") UUID id);
}
