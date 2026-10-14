package com.sunday.services.controller;

import com.sunday.common_lib.enums.CabinClassType;
import java.util.UUID;
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
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(service.create(userId, request));
    }

    @PostMapping("/bulk")
    public ResponseEntity<FlightCabinAncillaryBulkCreateResponse> bulkCreate(
            @Valid @RequestBody List<FlightCabinAncillaryRequest> requests,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(service.bulkCreate(userId, requests));
    }

    @GetMapping("/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}")
    public ResponseEntity<FlightCabinAncillaryResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/all")
    public ResponseEntity<List<FlightCabinAncillaryResponse>> getAllByIds(
            @RequestParam List<UUID> Ids) {
        return ResponseEntity.ok(service.getAllByIds(Ids));
    }

    @GetMapping("/flight/{flightId:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/cabin/{cabinClass}")
    public ResponseEntity<List<FlightCabinAncillaryResponse>> getAllByFlightAndCabinClass(
            @PathVariable UUID flightId,
            @PathVariable CabinClassType cabinClass) {
        return ResponseEntity.ok(service.getAllByFlightAndCabinClass(flightId, cabinClass));
    }

    @GetMapping("/flight/{flightId}/cabin/{cabinClass}/type/{type}")
    public ResponseEntity<FlightCabinAncillaryResponse> getByFlightAndCabinClassAndType(
            @PathVariable UUID flightId,
            @PathVariable CabinClassType cabinClass,
            @PathVariable AncillaryType type) {
        return ResponseEntity.ok(
                service.getByFlightIdAndCabinClassAndType(flightId, cabinClass, type));
    }

    @GetMapping("/flight/{flightId}/cabin/{cabinClass}/type/{type}/all")
    public ResponseEntity<?> getAllByFlightAndCabinClassAndType(
            @PathVariable UUID flightId,
            @PathVariable CabinClassType cabinClass,
            @PathVariable AncillaryType type) {
        return ResponseEntity.ok(
                service.getAllByFlightIdAndCabinClassAndType(flightId, cabinClass, type));
    }

    @PutMapping("/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}")
    public ResponseEntity<FlightCabinAncillaryResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody FlightCabinAncillaryRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(service.update(userId, id, request));
    }

    @DeleteMapping("/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        service.delete(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/price/total")
    public ResponseEntity<?> calculateAncillariesPrice(
            @RequestParam UUID flightId,
            @RequestParam CabinClassType cabinClass,
            @RequestParam int passengers,
            @RequestBody List<UUID> flightCabinAncillaryIds) {
        return ResponseEntity.ok(service.calculateAncillaryPrice(flightId, cabinClass, passengers, flightCabinAncillaryIds));
    }
}
