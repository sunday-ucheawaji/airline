package com.sunday.services.client;

import java.util.UUID;
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
    List<String> getMyPermissions(@PathVariable UUID airlineId, @RequestHeader("X-User-Id") UUID userId);

    @GetMapping("/api/airlines/permissions/check")
    void requirePermission(
            @RequestParam List<UUID> airlineIds,
            @RequestParam String permission,
            @RequestHeader("X-User-Id") UUID userId);

    @GetMapping("/internal/aircrafts/{id}")
    AircraftResponse getAircraftByIdInternal(@PathVariable("id") UUID id);

    @PostMapping("/internal/aircrafts/batch")
    Map<UUID, AircraftResponse> getAircraftsByIdsInternal(@RequestBody List<UUID> ids);
}
