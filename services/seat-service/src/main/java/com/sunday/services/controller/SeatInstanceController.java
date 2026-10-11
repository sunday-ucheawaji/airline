package com.sunday.services.controller;

import java.util.UUID;
import com.sunday.common_lib.enums.SeatAvailabilityStatus;
import com.sunday.common_lib.payload.request.SeatInstanceRequest;
import com.sunday.common_lib.payload.request.SeatPriceRequest;
import com.sunday.common_lib.payload.response.CabinSeatStatusResponse;
import com.sunday.common_lib.payload.response.SeatInstanceResponse;
import com.sunday.services.service.SeatInstanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/seat-instances")
@RequiredArgsConstructor
public class SeatInstanceController {

    private final SeatInstanceService seatInstanceService;

    @PostMapping
    public ResponseEntity<SeatInstanceResponse> createSeatInstance(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody SeatInstanceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(seatInstanceService.createSeatInstance(userId, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SeatInstanceResponse> getSeatInstanceById(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(seatInstanceService.getSeatInstanceById(userId, id));
    }

    @PostMapping("/price/total")
    public ResponseEntity<BigDecimal> calculateSeatPrice(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody SeatPriceRequest request) {
        return ResponseEntity.ok(seatInstanceService.calculateSeatPrice(userId, request.getSeatInstanceIds()));
    }

    @GetMapping("/flight/{flightId}")
    public ResponseEntity<List<SeatInstanceResponse>> getSeatInstancesByFlightId(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID flightId) {
        return ResponseEntity.ok(seatInstanceService.getSeatInstancesByFlightId(userId, flightId));
    }

    @GetMapping("/all")
    public ResponseEntity<List<SeatInstanceResponse>> getAllByIds(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam List<UUID> Ids) {
        return ResponseEntity.ok(seatInstanceService.getAllByIds(userId, Ids));
    }

    @GetMapping("/flight/{flightId}/available")
    public ResponseEntity<List<SeatInstanceResponse>> getAvailableSeatsByFlightId(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID flightId) {
        return ResponseEntity.ok(seatInstanceService.getAvailableSeatsByFlightId(userId, flightId));
    }

    @GetMapping("/flight/{flightId}/available/count")
    public ResponseEntity<Long> countAvailableByFlightId(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID flightId) {
        return ResponseEntity.ok(seatInstanceService.countAvailableByFlightId(userId, flightId));
    }

    @GetMapping("/flight-instance/{flightInstanceId}/status-summary")
    public ResponseEntity<List<CabinSeatStatusResponse>> getSeatStatusSummary(@PathVariable UUID flightInstanceId) {
        return ResponseEntity.ok(seatInstanceService.getSeatStatusSummary(flightInstanceId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<SeatInstanceResponse> updateSeatInstanceStatus(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id,
            @RequestParam SeatAvailabilityStatus status) {
        return ResponseEntity.ok(seatInstanceService.updateSeatInstanceStatus(userId, id, status));
    }

    @PostMapping("/{id}/release")
    public ResponseEntity<SeatInstanceResponse> releaseSeatInstance(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(seatInstanceService.releaseSeatInstance(userId, id));
    }
}
