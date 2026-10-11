package com.sunday.services.controller;

import java.util.UUID;
import com.sunday.common_lib.enums.AirlineStatus;
import com.sunday.common_lib.payload.request.AirlineRequest;
import com.sunday.common_lib.payload.response.AirlineDropdownItem;
import com.sunday.common_lib.payload.response.AirlineResponse;
import com.sunday.services.service.AirlineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/airlines")
@RequiredArgsConstructor
public class AirlineController {

    private final AirlineService airlineService;

    // ---------- CRUD ----------
    // Airlines are only ever created via the onboarding approval pipeline
    // (see OnboardingController) — there is no direct self-serve create here.

    @GetMapping("/mine")
    public ResponseEntity<List<AirlineResponse>> getMyAirlines(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(airlineService.getMyAirlines(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AirlineResponse> getAirlineById(
            @PathVariable UUID id, @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(airlineService.getAirlineById(id, userId));
    }

    @GetMapping("/{airlineId}/permissions")
    public ResponseEntity<List<String>> getMyPermissions(
            @PathVariable UUID airlineId,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(airlineService.getMyPermissions(airlineId, userId));
    }

    // Batch check for callers validating a permission across several airlines in one request
    // (e.g. a bulk create spanning multiple airlines) — one round trip instead of one per airline.
    @GetMapping("/permissions/check")
    public ResponseEntity<Void> requirePermission(
            @RequestParam List<UUID> airlineIds,
            @RequestParam String permission,
            @RequestHeader("X-User-Id") UUID userId) {
        airlineService.requirePermission(userId, airlineIds, permission);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<AirlineResponse>> getAllAirlines(Pageable pageable) {
        return ResponseEntity.ok(airlineService.getAllAirlines(pageable));
    }

    @GetMapping("/dropdown")
    public ResponseEntity<List<AirlineDropdownItem>> getAirlinesForDropdown() {
        return ResponseEntity.ok(airlineService.getAirlinesForDropdown());
    }

    @PutMapping("/{id}")
    public ResponseEntity<AirlineResponse> updateAirline(
            @PathVariable UUID id,
            @Valid @RequestBody AirlineRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(airlineService.updateAirline(id, request, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAirline(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        airlineService.deleteAirline(id, userId);
        return ResponseEntity.noContent().build();
    }

    // Reinstates a SUSPENDED/BANNED airline. Not an approval — onboarding approval already creates
    // airlines ACTIVE (see OnboardingController).
    @PostMapping("/{id}/activate")
    public ResponseEntity<AirlineResponse> activateAirline(@PathVariable UUID id) {
        return ResponseEntity.ok(airlineService.changeStatusByAdmin(id, AirlineStatus.ACTIVE));
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<AirlineResponse> suspendAirline(@PathVariable UUID id) {
        return ResponseEntity.ok(airlineService.changeStatusByAdmin(id, AirlineStatus.SUSPENDED));
    }

    @PostMapping("/{id}/ban")
    public ResponseEntity<AirlineResponse> banAirline(@PathVariable UUID id) {
        return ResponseEntity.ok(airlineService.changeStatusByAdmin(id, AirlineStatus.BANNED));
    }
}
