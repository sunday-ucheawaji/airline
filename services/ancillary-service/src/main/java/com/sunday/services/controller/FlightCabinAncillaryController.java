package com.sunday.services.controller;

import com.sunday.common_lib.enums.AncillaryType;
import com.sunday.common_lib.payload.request.FlightCabinAncillaryRequest;
import com.sunday.common_lib.payload.response.FlightCabinAncillaryBulkCreateResponse;
import com.sunday.common_lib.payload.response.FlightCabinAncillaryResponse;
import com.sunday.services.service.FlightCabinAncillaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/flight-cabin-ancillaries")
@RequiredArgsConstructor
public class FlightCabinAncillaryController {

    private final FlightCabinAncillaryService service;

    @PostMapping
    public ResponseEntity<FlightCabinAncillaryResponse> create(
            @Valid @RequestBody FlightCabinAncillaryRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(service.create(userId, request));
    }

    @PostMapping("/bulk")
    public ResponseEntity<FlightCabinAncillaryBulkCreateResponse> bulkCreate(
            @Valid @RequestBody List<FlightCabinAncillaryRequest> requests,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(service.bulkCreate(userId, requests));
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<FlightCabinAncillaryResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/all")
    public ResponseEntity<List<FlightCabinAncillaryResponse>> getAllByIds(
            @RequestParam List<Long> Ids) {
        return ResponseEntity.ok(service.getAllByIds(Ids));
    }

    @GetMapping("/flight/{flightId:\\d+}/cabin/{cabinClassId}")
    public ResponseEntity<List<FlightCabinAncillaryResponse>> getAllByFlightAndCabinClass(
            @PathVariable Long flightId,
            @PathVariable Long cabinClassId) {
        return ResponseEntity.ok(service.getAllByFlightAndCabinClass(flightId, cabinClassId));
    }

    @GetMapping("/flight/{flightId}/cabin/{cabinClassId}/type/{type}")
    public ResponseEntity<FlightCabinAncillaryResponse> getByFlightAndCabinClassAndType(
            @PathVariable Long flightId,
            @PathVariable Long cabinClassId,
            @PathVariable AncillaryType type) {
        return ResponseEntity.ok(
                service.getByFlightIdAndCabinClassAndType(flightId, cabinClassId, type));
    }

    @GetMapping("/flight/{flightId}/cabin/{cabinClassId}/type/{type}/all")
    public ResponseEntity<?> getAllByFlightAndCabinClassAndType(
            @PathVariable Long flightId,
            @PathVariable Long cabinClassId,
            @PathVariable AncillaryType type) {
        return ResponseEntity.ok(
                service.getAllByFlightIdAndCabinClassAndType(flightId, cabinClassId, type));
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<FlightCabinAncillaryResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody FlightCabinAncillaryRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(service.update(userId, id, request));
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        service.delete(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/price/total")
    public ResponseEntity<?> calculateAncillariesPrice(
            @RequestBody List<Long> flightCabinAncillaryIds) {
        return ResponseEntity.ok(service.calculateAncillaryPrice(flightCabinAncillaryIds));
    }
}
