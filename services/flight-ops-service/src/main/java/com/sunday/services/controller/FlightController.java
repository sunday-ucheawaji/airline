package com.sunday.services.controller;

import java.util.UUID;
import com.sunday.common_lib.enums.FlightStatus;
import com.sunday.common_lib.payload.request.FlightRequest;
import com.sunday.common_lib.payload.response.FlightResponse;
import com.sunday.services.service.FlightService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/flights")
@RequiredArgsConstructor
public class FlightController {

    private final FlightService flightService;

    @PostMapping
    public ResponseEntity<FlightResponse> createFlight(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody FlightRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(flightService.createFlight(userId, request));
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<FlightResponse>> createFlights(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody List<FlightRequest> requests) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(flightService.createFlights(userId, requests));
    }

    @PostMapping("/batch")
    public ResponseEntity<Map<UUID, FlightResponse>> getFlightsByIds(@RequestBody List<UUID> ids) {
        return ResponseEntity.ok(flightService.getFlightsByIds(ids));
    }

    @GetMapping("/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}")
    public ResponseEntity<FlightResponse> getFlightById(@PathVariable UUID id)  {
        return ResponseEntity.ok(flightService.getFlightById(id));
    }

    @GetMapping("/number/{flightNumber}")
    public ResponseEntity<FlightResponse> getFlightByNumber(
            @PathVariable String flightNumber) {
        return ResponseEntity.ok(flightService.getFlightByNumber(flightNumber));
    }

    @GetMapping("/airline")
    public ResponseEntity<Page<FlightResponse>> getFlightsByAirline(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam UUID airlineId,
            @RequestParam(required = false) UUID departureAirportId,
            @RequestParam(required = false) UUID arrivalAirportId,
            Pageable pageable) {
        return ResponseEntity.ok(flightService.getFlightsByAirline(
                userId,
                airlineId,
                departureAirportId,
                arrivalAirportId,
                pageable
        ));
    }

    @PutMapping("/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}")
    public ResponseEntity<FlightResponse> updateFlight(
            @PathVariable UUID id,
            @Valid @RequestBody FlightRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(flightService.updateFlight(id, request, userId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<FlightResponse> changeStatus(
            @PathVariable UUID id,
            @RequestParam FlightStatus status,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(flightService.changeStatus(id, status, userId));
    }

    @DeleteMapping("/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}")
    public ResponseEntity<Void> deleteFlight(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        flightService.deleteFlight(id, userId);
        return ResponseEntity.noContent().build();
    }

}
