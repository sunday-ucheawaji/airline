package com.sunday.services.controller;

import java.util.UUID;
import com.sunday.common_lib.payload.request.InsuranceCoverageRequest;
import com.sunday.common_lib.payload.response.ApiResponse;
import com.sunday.common_lib.payload.response.InsuranceCoverageBulkCreateResponse;
import com.sunday.common_lib.payload.response.InsuranceCoverageResponse;
import com.sunday.services.service.InsuranceCoverageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/insurance-coverages")
@RequiredArgsConstructor
public class InsuranceCoverageController {

    private final InsuranceCoverageService coverageService;

    @PostMapping
    public ResponseEntity<InsuranceCoverageResponse> createCoverage(
            @Valid @RequestBody InsuranceCoverageRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        InsuranceCoverageResponse response = coverageService.createCoverage(userId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/bulk")
    public ResponseEntity<InsuranceCoverageBulkCreateResponse> createCoveragesBulk(
            @Valid @RequestBody List<InsuranceCoverageRequest> requests,
            @RequestHeader("X-User-Id") UUID userId) {
        return new ResponseEntity<>(coverageService.createCoveragesBulk(userId, requests), HttpStatus.CREATED);
    }

    @PutMapping("/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}")
    public ResponseEntity<InsuranceCoverageResponse> updateCoverage(
            @PathVariable UUID id,
            @Valid @RequestBody InsuranceCoverageRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(coverageService.updateCoverage(userId, id, request));
    }

    @DeleteMapping("/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}")
    public ResponseEntity<ApiResponse> deleteCoverage(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        coverageService.deleteCoverage(userId, id);
        return ResponseEntity.ok(new ApiResponse("Coverage deleted successfully"));
    }

    @GetMapping("/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}")
    public ResponseEntity<InsuranceCoverageResponse> getCoverageById(@PathVariable UUID id) {
        return ResponseEntity.ok(coverageService.getCoverageById(id));
    }

    @GetMapping
    public ResponseEntity<List<InsuranceCoverageResponse>> getAllCoverages() {
        return ResponseEntity.ok(coverageService.getAllCoverages());
    }

    @GetMapping("/ancillary/{ancillaryId:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}")
    public ResponseEntity<List<InsuranceCoverageResponse>> getCoveragesByAncillaryId(
            @PathVariable UUID ancillaryId) {
        return ResponseEntity.ok(coverageService.getCoveragesByAncillaryId(ancillaryId));
    }

    @GetMapping("/ancillary/{ancillaryId:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/active")
    public ResponseEntity<List<InsuranceCoverageResponse>> getActiveCoveragesByAncillaryId(
            @PathVariable UUID ancillaryId) {
        return ResponseEntity.ok(coverageService.getActiveCoveragesByAncillaryId(ancillaryId));
    }
}
