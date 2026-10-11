package com.sunday.services.controller;

import java.util.UUID;
import com.sunday.common_lib.payload.request.FareRulesRequest;
import com.sunday.common_lib.payload.response.FareRulesResponse;
import com.sunday.services.service.FareRulesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fare-rules")
@RequiredArgsConstructor
public class FareRulesController {

    private final FareRulesService fareRulesService;

    @PostMapping
    public ResponseEntity<FareRulesResponse> createFareRules(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody FareRulesRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fareRulesService.createFareRules(userId, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FareRulesResponse> getFareRulesById(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(fareRulesService.getFareRulesById(userId, id));
    }

    @GetMapping("/fare/{fareId}")
    public ResponseEntity<FareRulesResponse> getFareRulesByFareId(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID fareId) {
        return ResponseEntity.ok(fareRulesService.getFareRulesByFareId(userId, fareId));
    }

    @GetMapping("/airline/{airlineId}")
    public ResponseEntity<Page<FareRulesResponse>> getFareRulesByAirlineId(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID airlineId,
            Pageable pageable) {
        return ResponseEntity.ok(fareRulesService.getFareRulesByAirlineId(userId, airlineId, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FareRulesResponse> updateFareRules(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody FareRulesRequest request) {
        return ResponseEntity.ok(fareRulesService.updateFareRules(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFareRules(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id) {
        fareRulesService.deleteFareRules(userId, id);
        return ResponseEntity.noContent().build();
    }
}
