package com.sunday.services.controller;

import com.sunday.common_lib.payload.request.BaggagePolicyRequest;
import com.sunday.common_lib.payload.response.BaggagePolicyBulkCreateResponse;
import com.sunday.common_lib.payload.response.BaggagePolicyResponse;
import com.sunday.services.service.BaggagePolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/baggage-policies")
@RequiredArgsConstructor
public class BaggagePolicyController {

    private final BaggagePolicyService baggagePolicyService;

    @PostMapping
    public ResponseEntity<BaggagePolicyResponse> createBaggagePolicy(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody BaggagePolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(baggagePolicyService.createBaggagePolicy(userId, request));
    }

    @PostMapping("/bulk")
    public ResponseEntity<BaggagePolicyBulkCreateResponse> createBaggagePolicies(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody List<BaggagePolicyRequest> requests) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(baggagePolicyService.createBaggagePolicies(userId, requests));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaggagePolicyResponse> getBaggagePolicyById(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        return ResponseEntity.ok(baggagePolicyService.getBaggagePolicyById(userId, id));
    }

    @GetMapping("/fare/{fareId}")
    public ResponseEntity<BaggagePolicyResponse> getBaggagePolicyByFareId(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long fareId) {
        return ResponseEntity.ok(baggagePolicyService.getBaggagePolicyByFareId(userId, fareId));
    }

    @GetMapping("/airline/{airlineId}")
    public ResponseEntity<Page<BaggagePolicyResponse>> getBaggagePoliciesByAirlineId(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long airlineId,
            Pageable pageable) {
        return ResponseEntity.ok(baggagePolicyService.getBaggagePoliciesByAirlineId(userId, airlineId, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BaggagePolicyResponse> updateBaggagePolicy(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id,
            @Valid @RequestBody BaggagePolicyRequest request) {
        return ResponseEntity.ok(baggagePolicyService.updateBaggagePolicy(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBaggagePolicy(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        baggagePolicyService.deleteBaggagePolicy(userId, id);
        return ResponseEntity.noContent().build();
    }
}
