package com.sunday.services.client;

import com.sunday.common_lib.payload.response.AircraftResponse;
import com.sunday.common_lib.payload.response.AirlineResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "airline-core-service", fallback = AirlineClientFallback.class)
public interface AirlineClient {

    @GetMapping("/api/airlines/mine")
    List<AirlineResponse> getMyAirlines(@RequestHeader("X-User-Id") Long userId);

    @GetMapping("/api/airlines/{airlineId}/permissions")
    List<String> getMyPermissions(@PathVariable Long airlineId, @RequestHeader("X-User-Id") Long userId);

    @GetMapping("/api/airlines/permissions/check")
    void requirePermission(
            @RequestParam List<Long> airlineIds,
            @RequestParam String permission,
            @RequestHeader("X-User-Id") Long userId);

    @GetMapping("/api/aircrafts/{id}")
    AircraftResponse getAircraftById(@PathVariable("id") Long id);
}
