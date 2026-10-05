package com.sunday.services.controller;

import com.sunday.common_lib.payload.request.SeatMapRequest;
import com.sunday.common_lib.payload.response.SeatMapBulkCreateResponse;
import com.sunday.common_lib.payload.response.SeatMapResponse;
import com.sunday.services.service.SeatMapService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seat-maps")
@RequiredArgsConstructor
public class SeatMapController {

    private final SeatMapService seatMapService;

    @PostMapping
    public ResponseEntity<SeatMapResponse> createSeatMap(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody SeatMapRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(seatMapService.createSeatMap(userId, request));
    }

    @PostMapping("/create/bulk")
    public ResponseEntity<SeatMapBulkCreateResponse> createSeatMaps(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody List<SeatMapRequest> requests) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(seatMapService.createSeatMaps(userId, requests));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SeatMapResponse> getSeatMapById(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        return ResponseEntity.ok(seatMapService.getSeatMapById(userId, id));
    }

    @GetMapping("/cabin-class/{cabinClassId}")
    public ResponseEntity<SeatMapResponse> getSeatMapsByCabinClass(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long cabinClassId) {
        return ResponseEntity.ok(seatMapService.getSeatMapsByCabinClass(userId, cabinClassId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SeatMapResponse> updateSeatMap(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id,
            @Valid @RequestBody SeatMapRequest request) {
        return ResponseEntity.ok(seatMapService.updateSeatMap(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSeatMap(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        seatMapService.deleteSeatMap(userId, id);
        return ResponseEntity.noContent().build();
    }
}
