package com.sunday.services.controller;

import com.sunday.common_lib.payload.request.FlightMealRequest;
import com.sunday.common_lib.payload.response.FlightMealBulkCreateResponse;
import com.sunday.common_lib.payload.response.FlightMealResponse;
import com.sunday.services.service.FlightMealService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/flight-meals")
@RequiredArgsConstructor
public class FlightMealController {

    private final FlightMealService flightMealService;

    @PostMapping
    public ResponseEntity<FlightMealResponse> createFlightMeal(
            @Valid @RequestBody FlightMealRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        FlightMealResponse response = flightMealService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/bulk")
    public ResponseEntity<FlightMealBulkCreateResponse> bulkCreateFlightMeals(
            @Valid @RequestBody List<FlightMealRequest> requests,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(flightMealService.bulkCreate(userId, requests));
    }

    @PostMapping("/price/total")
    public ResponseEntity<Double> calculateMealPrice(
            @RequestBody List<Long> requests) {
        double responses = flightMealService.calculateMealPrice(requests);
        return ResponseEntity.status(HttpStatus.OK).body(responses);
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<FlightMealResponse> getFlightMealById(@PathVariable Long id) {
        return ResponseEntity.ok(flightMealService.getById(id));
    }

    @GetMapping("/flight/{flightId:\\d+}")
    public ResponseEntity<List<FlightMealResponse>> getMealsByFlightId(
            @PathVariable Long flightId) {
        return ResponseEntity.ok(flightMealService.getByFlightId(flightId));
    }

    @GetMapping("/all")
    public ResponseEntity<List<FlightMealResponse>> getMealsByIds(
            @RequestParam List<Long> Ids) {
        return ResponseEntity.ok(flightMealService.getAllByIds(Ids));
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<FlightMealResponse> updateFlightMeal(
            @PathVariable Long id,
            @Valid @RequestBody FlightMealRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(flightMealService.update(userId, id, request));
    }

    @PatchMapping("/{id:\\d+}/availability")
    public ResponseEntity<FlightMealResponse> updateFlightMealAvailability(
            @PathVariable Long id,
            @RequestParam Boolean available,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(flightMealService.updateAvailability(userId, id, available));
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> deleteFlightMeal(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        flightMealService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
