package com.sunday.services.controller;

import com.sunday.common_lib.payload.request.MealRequest;
import com.sunday.common_lib.payload.response.MealBulkCreateResponse;
import com.sunday.common_lib.payload.response.MealResponse;
import com.sunday.services.service.MealService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/meals")
@RequiredArgsConstructor
public class MealController {

    private final MealService mealService;

    @PostMapping
    public ResponseEntity<MealResponse> createMeal(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody MealRequest request) {
        MealResponse response = mealService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/bulk")
    public ResponseEntity<MealBulkCreateResponse> bulkCreateMeals(
            @Valid @RequestBody List<MealRequest> requests,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mealService.bulkCreate(userId, requests));
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<MealResponse> getMealById(@PathVariable Long id) {
        return ResponseEntity.ok(mealService.getById(id));
    }

    @GetMapping("/airline")
    public ResponseEntity<List<MealResponse>> getMealsByAirlineId(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam Long airlineId) {
        return ResponseEntity.ok(mealService.getByAirlineId(userId, airlineId));
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<MealResponse> updateMeal(
            @PathVariable Long id,
            @Valid @RequestBody MealRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(mealService.update(userId, id, request));
    }

    @PatchMapping("/{id:\\d+}/availability")
    public ResponseEntity<MealResponse> updateMealAvailability(
            @PathVariable Long id,
            @RequestParam Boolean available,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(mealService.updateAvailability(userId, id, available));
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> deleteMeal(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        mealService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
