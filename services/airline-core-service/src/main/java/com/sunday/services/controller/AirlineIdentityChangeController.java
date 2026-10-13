package com.sunday.services.controller;

import com.sunday.common_lib.payload.request.AirlineIdentityChangeRequestCreate;
import com.sunday.common_lib.payload.response.AirlineIdentityChangeResponse;
import com.sunday.services.service.AirlineIdentityChangeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/airlines/{airlineId}/identity-change-requests")
@RequiredArgsConstructor
public class AirlineIdentityChangeController {

    private final AirlineIdentityChangeService identityChangeService;

    @PostMapping
    public ResponseEntity<AirlineIdentityChangeResponse> request(
            @PathVariable UUID airlineId,
            @Valid @RequestBody AirlineIdentityChangeRequestCreate request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(identityChangeService.request(airlineId, request, userId));
    }

    @GetMapping
    public ResponseEntity<List<AirlineIdentityChangeResponse>> list(
            @PathVariable UUID airlineId, @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(identityChangeService.listForAirline(airlineId, userId));
    }

    @PostMapping("/{requestId}/cancel")
    public ResponseEntity<AirlineIdentityChangeResponse> cancel(
            @PathVariable UUID airlineId, @PathVariable UUID requestId, @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(identityChangeService.cancel(airlineId, requestId, userId));
    }
}
