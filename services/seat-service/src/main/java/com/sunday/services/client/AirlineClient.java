package com.sunday.services.client;

import com.sunday.common_lib.payload.response.AircraftResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "airline-core-service", fallback = AirlineClientFallback.class)
public interface AirlineClient {

    @GetMapping("/api/airlines/{airlineId}/permissions")
    List<String> getMyPermissions(@PathVariable Long airlineId, @RequestHeader("X-User-Id") Long userId);

    @GetMapping("/api/airlines/permissions/check")
    void requirePermission(
            @RequestParam List<Long> airlineIds,
            @RequestParam String permission,
            @RequestHeader("X-User-Id") Long userId);

    @GetMapping("/internal/aircrafts/{id}")
    AircraftResponse getAircraftByIdInternal(@PathVariable("id") Long id);

    @PostMapping("/internal/aircrafts/batch")
    Map<Long, AircraftResponse> getAircraftsByIdsInternal(@RequestBody List<Long> ids);
}
