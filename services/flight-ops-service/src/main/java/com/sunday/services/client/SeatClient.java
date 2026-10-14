package com.sunday.services.client;

import java.util.UUID;
import com.sunday.common_lib.payload.request.CabinAvailabilityRequest;
import com.sunday.common_lib.payload.response.CabinClassResponse;
import com.sunday.common_lib.payload.response.CabinSeatStatusResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "seat-service", fallback = SeatClientFallback.class)
public interface SeatClient {

    @GetMapping("/internal/cabin-classes/aircraft/{aircraftId}")
    List<CabinClassResponse> getCabinClassesByAircraftId(
            @PathVariable UUID aircraftId);

    /** Per-cabin count of seats already sold/held (status != AVAILABLE) for one flight instance — used to validate an aircraft reassignment before committing it. */
    @GetMapping("/internal/seat-instances/flight-instance/{flightInstanceId}/status-summary")
    List<CabinSeatStatusResponse> getSeatStatusSummary(@PathVariable UUID flightInstanceId);

    /** Free seats in one cabin per flight instance — one call for a whole search page. */
    @PostMapping("/internal/seat-instances/availability")
    Map<UUID, Long> countAvailableSeats(@RequestBody CabinAvailabilityRequest request);

    /** Seats held, booked or occupied per flight instance. */
    @PostMapping("/internal/seat-instances/sold-counts")
    Map<UUID, Long> countSoldSeats(@RequestBody List<UUID> flightInstanceIds);
}
