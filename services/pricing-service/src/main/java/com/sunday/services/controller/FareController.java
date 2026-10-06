package com.sunday.services.controller;

import com.sunday.common_lib.payload.request.FareRequest;
import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.common_lib.payload.response.FareBulkCreateResponse;
import com.sunday.common_lib.payload.response.FareOfferResponse;
import com.sunday.common_lib.payload.response.FareResponse;
import com.sunday.services.service.FareLifecycleService;
import com.sunday.services.service.FareOfferService;
import com.sunday.services.service.FareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fares")
@RequiredArgsConstructor
public class FareController {

    private final FareService fareService;
    private final FareLifecycleService fareLifecycleService;
    private final FareOfferService fareOfferService;

    @PostMapping
    public ResponseEntity<FareResponse> createFare(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody FareRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fareService.createFare(userId, request));
    }

    @PostMapping("/bulk")
    public ResponseEntity<FareBulkCreateResponse> createFares(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody List<FareRequest> requests) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fareService.createFares(userId, requests));
    }

    @GetMapping
    public ResponseEntity<Page<FareResponse>> getFaresByAirline(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam Long airlineId,
            Pageable pageable) {
        return ResponseEntity.ok(fareService.getFaresByAirline(userId, airlineId, pageable));
    }

    // Public customer read: published, in-window fares on one flight and cabin. No airline permission is needed.
    @GetMapping("/offers")
    public ResponseEntity<List<FareOfferResponse>> offers(
            @RequestParam Long flightId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureDate,
            @RequestParam CabinClassType cabinClass) {
        return ResponseEntity.ok(fareOfferService.offers(flightId, departureDate, cabinClass));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FareResponse> getFareById(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        return ResponseEntity.ok(fareService.getFareById(userId, id));
    }

    @GetMapping("/flight/{flightId}/cabin-class/{cabinClassId}")
    public ResponseEntity<List<FareResponse>> getFaresByFlightAndCabinClass(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long flightId,
            @PathVariable Long cabinClassId) {
        return ResponseEntity.ok(fareService.getFaresByFlightIdAndCabinClassId(userId, flightId, cabinClassId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FareResponse> updateFare(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id,
            @Valid @RequestBody FareRequest request) {
        return ResponseEntity.ok(fareService.updateFare(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFare(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        fareService.deleteFare(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<FareResponse> publishFare(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        return ResponseEntity.ok(fareLifecycleService.publishFare(userId, id));
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<FareResponse> withdrawFare(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        return ResponseEntity.ok(fareLifecycleService.withdrawFare(userId, id));
    }
}
