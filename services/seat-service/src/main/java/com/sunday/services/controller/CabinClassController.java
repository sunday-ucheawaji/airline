package com.sunday.services.controller;

import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.common_lib.payload.request.CabinClassRequest;
import com.sunday.common_lib.payload.response.CabinClassBulkCreateResponse;
import com.sunday.common_lib.payload.response.CabinClassResponse;
import com.sunday.services.service.CabinClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cabin-classes")
@RequiredArgsConstructor
public class CabinClassController {

    private final CabinClassService cabinClassService;

    @PostMapping
    public ResponseEntity<CabinClassResponse> createCabinClass(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CabinClassRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cabinClassService.createCabinClass(userId, request));
    }

    @PostMapping("/create/bulk")
    public ResponseEntity<CabinClassBulkCreateResponse> createCabinClasses(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody List<CabinClassRequest> requests) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cabinClassService.createCabinClasses(userId, requests));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CabinClassResponse> getCabinClassById(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        return ResponseEntity.ok(cabinClassService.getCabinClassById(userId, id));
    }

    @GetMapping("/aircraft/{id}/name/{cabinClass}")
    public ResponseEntity<CabinClassResponse> getCabinClassByAircraftIdAndName(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable CabinClassType cabinClass,
            @PathVariable Long id) {
        return ResponseEntity.ok(
                cabinClassService.getByAircraftIdAndName(userId, id, cabinClass));
    }

    @GetMapping("/aircraft/{aircraftId}")
    public ResponseEntity<List<CabinClassResponse>> getCabinClassesByAircraftId(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long aircraftId) {
        return ResponseEntity.ok(cabinClassService.getCabinClassesByAircraftId(userId, aircraftId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CabinClassResponse> updateCabinClass(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id,
            @Valid @RequestBody CabinClassRequest request) {
        return ResponseEntity.ok(cabinClassService.updateCabinClass(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCabinClass(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        cabinClassService.deleteCabinClass(userId, id);
        return ResponseEntity.noContent().build();
    }
}
