package com.sunday.services.controller;

import java.util.UUID;
import com.sunday.common_lib.payload.request.FlightMealRequest;
import com.sunday.common_lib.payload.response.FlightMealBulkCreateResponse;
import com.sunday.common_lib.payload.response.FlightMealResponse;
import com.sunday.services.service.FlightMealService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/flight-meals")
@RequiredArgsConstructor
public class FlightMealController {

    private final FlightMealService flightMealService;

    @PostMapping
    public ResponseEntity<FlightMealResponse> createFlightMeal(
            @Valid @RequestBody FlightMealRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        FlightMealResponse response = flightMealService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/bulk")
    public ResponseEntity<FlightMealBulkCreateResponse> bulkCreateFlightMeals(
            @Valid @RequestBody List<FlightMealRequest> requests,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(flightMealService.bulkCreate(userId, requests));
    }

    @PostMapping("/price/total")
    public ResponseEntity<BigDecimal> calculateMealPrice(
            @RequestParam UUID flightId,
            @RequestBody List<UUID> requests) {
        BigDecimal responses = flightMealService.calculateMealPrice(flightId, requests);
        return ResponseEntity.status(HttpStatus.OK).body(responses);
    }

    @GetMapping("/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}")
    public ResponseEntity<FlightMealResponse> getFlightMealById(@PathVariable UUID id) {
        return ResponseEntity.ok(flightMealService.getById(id));
    }

    @GetMapping("/flight/{flightId:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}")
    public ResponseEntity<List<FlightMealResponse>> getMealsByFlightId(
            @PathVariable UUID flightId) {
        return ResponseEntity.ok(flightMealService.getByFlightId(flightId));
    }

    @GetMapping("/all")
    public ResponseEntity<List<FlightMealResponse>> getMealsByIds(
            @RequestParam List<UUID> Ids) {
        return ResponseEntity.ok(flightMealService.getAllByIds(Ids));
    }

    @PutMapping("/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}")
    public ResponseEntity<FlightMealResponse> updateFlightMeal(
            @PathVariable UUID id,
            @Valid @RequestBody FlightMealRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(flightMealService.update(userId, id, request));
    }

    @PatchMapping("/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/availability")
    public ResponseEntity<FlightMealResponse> updateFlightMealAvailability(
            @PathVariable UUID id,
            @RequestParam Boolean available,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(flightMealService.updateAvailability(userId, id, available));
    }

    @DeleteMapping("/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}")
    public ResponseEntity<Void> deleteFlightMeal(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        flightMealService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
