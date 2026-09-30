package com.sunday.services.client;

import com.sunday.common_lib.payload.response.AircraftResponse;
import com.sunday.common_lib.payload.response.AirlineResponse;
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

    // Batch check for callers validating a permission across several airlines in one request
    // (e.g. a bulk create spanning multiple airlines) — one round trip instead of one per airline.
    @GetMapping("/api/airlines/permissions/check")
    void requirePermission(
            @RequestParam List<Long> airlineIds,
            @RequestParam String permission,
            @RequestHeader("X-User-Id") Long userId);

    /** Unauthenticated, service-to-service only — for display-enrichment callers with no real caller identity to forward (airline-core-service's GET /api/airlines/{id} requires an active membership). */
    @GetMapping("/internal/airlines/{id}")
    AirlineResponse getAirlineByIdInternal(@PathVariable("id") Long id);

    /** Batch variant of {@link #getAirlineByIdInternal}: one round trip instead of one per airline. */
    @PostMapping("/internal/airlines/batch")
    Map<Long, AirlineResponse> getAirlinesByIdsInternal(@RequestBody List<Long> ids);

    /** Permission-gated post-Aircraft-rework — requires the caller's real identity, used only on mutation/ownership-sensitive paths. */
    @GetMapping("/api/aircrafts/{id}")
    AircraftResponse getAircraftById(@PathVariable("id") Long id, @RequestHeader("X-User-Id") Long userId);

    /** Unauthenticated, service-to-service only — for display-enrichment callers (search, batch responses) with no real caller identity to forward. */
    @GetMapping("/internal/aircrafts/{id}")
    AircraftResponse getAircraftByIdInternal(@PathVariable("id") Long id);

    /** Batch variant of {@link #getAircraftByIdInternal}: one round trip instead of one per aircraft. */
    @PostMapping("/internal/aircrafts/batch")
    Map<Long, AircraftResponse> getAircraftsByIdsInternal(@RequestBody List<Long> ids);

    /**
     * Bulk-resolves a list of IATA codes to {@link AirlineResponse} objects.
     * Used during flight search to translate airline filter codes to IDs.
     */
    @GetMapping("/api/airlines/by-iata")
    List<AirlineResponse> getAirlinesByIataCodes(@RequestParam("codes") List<String> codes);

    /**
     * Returns all airlines belonging to the given alliance name.
     * Used during flight search to apply the alliance filter.
     */
    @GetMapping("/api/airlines/by-alliance")
    List<AirlineResponse> getAirlinesByAlliance(@RequestParam("alliance") String alliance);
}
