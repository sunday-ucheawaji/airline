package com.sunday.services.controller;

import java.util.UUID;
import com.sunday.common_lib.payload.request.FlightScheduleRequest;
import com.sunday.common_lib.payload.response.FlightScheduleResponse;
import com.sunday.common_lib.payload.response.ScheduleChangeResponse;
import com.sunday.services.service.FlightScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/flight-schedules")
@RequiredArgsConstructor
public class FlightScheduleController {

    private final FlightScheduleService flightScheduleService;

    @PostMapping
    public ResponseEntity<FlightScheduleResponse> createFlightSchedule(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody FlightScheduleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        flightScheduleService
                                .createFlightSchedule(userId,request)
                );
    }

    @GetMapping("/{id}")
    public ResponseEntity<FlightScheduleResponse> getFlightScheduleById(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(
                flightScheduleService.getFlightScheduleById(id, userId)
        );
    }

    @GetMapping
    public ResponseEntity<List<FlightScheduleResponse>> getFlightSchedules(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam UUID airlineId
    ) {
        return ResponseEntity.ok(
                flightScheduleService.getFlightScheduleByAirline(userId, airlineId)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ScheduleChangeResponse> updateFlightSchedule(
            @PathVariable UUID id,
            @Valid @RequestBody FlightScheduleRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(flightScheduleService.updateFlightSchedule(id, request, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ScheduleChangeResponse> deleteFlightSchedule(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(flightScheduleService.deleteFlightSchedule(id, userId));
    }

}
