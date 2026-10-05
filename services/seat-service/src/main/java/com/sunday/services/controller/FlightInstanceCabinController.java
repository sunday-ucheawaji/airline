package com.sunday.services.controller;

import com.sunday.common_lib.payload.request.FlightInstanceCabinRequest;
import com.sunday.common_lib.payload.response.FlightInstanceCabinResponse;
import com.sunday.services.service.FlightInstanceCabinService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/flight-instance-cabins")
@RequiredArgsConstructor
public class FlightInstanceCabinController {

    private final FlightInstanceCabinService flightInstanceCabinService;

    @PostMapping
    public ResponseEntity<FlightInstanceCabinResponse> createFlightInstanceCabin(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody FlightInstanceCabinRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(flightInstanceCabinService.createFlightInstanceCabin(userId, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FlightInstanceCabinResponse> getFlightInstanceCabinById(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        return ResponseEntity.ok(flightInstanceCabinService.getFlightInstanceCabinById(userId, id));
    }

    @GetMapping("/flight-instance/{flightInstanceId}/cabin-class/{cabinClassId}")
    public ResponseEntity<FlightInstanceCabinResponse> getByFlightInstanceIdAndCabinClassId(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long cabinClassId,
            @PathVariable Long flightInstanceId) {
        return ResponseEntity.ok(
                flightInstanceCabinService.getByFlightInstanceIdAndCabinClassId(userId, flightInstanceId, cabinClassId));
    }

    @GetMapping("/flight-instance/{flightInstanceId}")
    public ResponseEntity<Page<FlightInstanceCabinResponse>> getByFlightInstanceId(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long flightInstanceId, Pageable pageable) {
        return ResponseEntity.ok(
                flightInstanceCabinService.getByFlightInstanceId(userId, flightInstanceId, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FlightInstanceCabinResponse> updateFlightInstanceCabin(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id,
            @Valid @RequestBody FlightInstanceCabinRequest request) {
        return ResponseEntity.ok(
                flightInstanceCabinService.updateFlightInstanceCabin(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFlightInstanceCabin(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        flightInstanceCabinService.deleteFlightInstanceCabin(userId, id);
        return ResponseEntity.noContent().build();
    }
}
