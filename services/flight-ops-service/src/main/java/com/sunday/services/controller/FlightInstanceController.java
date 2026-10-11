package com.sunday.services.controller;

import java.util.UUID;
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
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody FlightInstanceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(flightInstanceService
                        .createFlightInstanceWithCabins(userId,request));
    }

    @PostMapping("/batch")
    public ResponseEntity<Map<UUID, FlightInstanceResponse>> getFlightInstancesByIds(@RequestBody List<UUID> ids) {
        return ResponseEntity.ok(flightInstanceService.getFlightInstancesByIds(ids));
    }

    @GetMapping("/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}")
    public ResponseEntity<FlightInstanceResponse> getFlightInstanceById(@PathVariable UUID id) {
        return ResponseEntity.ok(flightInstanceService.getFlightInstanceById(id));
    }

    @GetMapping()
    public ResponseEntity<Page<FlightInstanceResponse>> getByAirlineId(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam UUID airlineId,
            @RequestParam(required = false) UUID departureAirportId,
            @RequestParam(required = false) UUID arrivalAirportId,
            @RequestParam(required = false) UUID flightId,
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

    @PutMapping("/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}")
    public ResponseEntity<FlightInstanceResponse> updateFlightInstance(
            @PathVariable UUID id,
            @Valid @RequestBody FlightInstanceRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(flightInstanceService.updateFlightInstance(id, request, userId));
    }

    @DeleteMapping("/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}")
    public ResponseEntity<Void> deleteFlightInstance(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        flightInstanceService.deleteFlightInstance(id, userId);
        return ResponseEntity.noContent().build();
    }

    /** Reassign the aircraft on an already-created instance (aircraft swaps, maintenance substitutions) — a narrower, dedicated permission from general instance management. */
    @PatchMapping("/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/aircraft")
    public ResponseEntity<FlightInstanceResponse> reassignAircraft(
            @PathVariable UUID id,
            @RequestBody @Valid ReassignAircraftRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(flightInstanceService.reassignAircraft(id, request.aircraftId(), userId));
    }

    public record ReassignAircraftRequest(@NotNull UUID aircraftId) {}
}
