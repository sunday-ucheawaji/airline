package com.sunday.services.controller;

import com.sunday.common_lib.payload.request.FlightInstanceRequest;
import com.sunday.common_lib.payload.response.FlightInstanceResponse;
import com.sunday.services.service.FlightInstanceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/flight-instances")
@RequiredArgsConstructor
public class FlightInstanceController {

    private final FlightInstanceService flightInstanceService;

    @PostMapping
    public ResponseEntity<FlightInstanceResponse> createFlightInstance(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody FlightInstanceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(flightInstanceService
                        .createFlightInstanceWithCabins(userId,request));
    }

    @PostMapping("/batch")
    public ResponseEntity<Map<Long, FlightInstanceResponse>> getFlightInstancesByIds(@RequestBody List<Long> ids) {
        return ResponseEntity.ok(flightInstanceService.getFlightInstancesByIds(ids));
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<FlightInstanceResponse> getFlightInstanceById(@PathVariable Long id) {
        return ResponseEntity.ok(flightInstanceService.getFlightInstanceById(id));
    }

    @GetMapping()
    public ResponseEntity<Page<FlightInstanceResponse>> getByAirlineId(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam Long airlineId,
            @RequestParam(required = false) Long departureAirportId,
            @RequestParam(required = false) Long arrivalAirportId,
            @RequestParam(required = false) Long flightId,
            @RequestParam(required = false) LocalDate onDate,
            Pageable pageable) {
        return ResponseEntity.ok(flightInstanceService.getByAirlineId(
                userId,
                airlineId,
                departureAirportId,
                arrivalAirportId,
                flightId,
                onDate,
                pageable));
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<FlightInstanceResponse> updateFlightInstance(
            @PathVariable Long id,
            @Valid @RequestBody FlightInstanceRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(flightInstanceService.updateFlightInstance(id, request, userId));
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> deleteFlightInstance(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        flightInstanceService.deleteFlightInstance(id, userId);
        return ResponseEntity.noContent().build();
    }

    /** Reassign the aircraft on an already-created instance (aircraft swaps, maintenance substitutions) — a narrower, dedicated permission from general instance management. */
    @PatchMapping("/{id:\\d+}/aircraft")
    public ResponseEntity<FlightInstanceResponse> reassignAircraft(
            @PathVariable Long id,
            @RequestBody @Valid ReassignAircraftRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(flightInstanceService.reassignAircraft(id, request.aircraftId(), userId));
    }

    public record ReassignAircraftRequest(@NotNull Long aircraftId) {}
}
