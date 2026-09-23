package com.sunday.services.controller;

import com.sunday.common_lib.payload.request.AncillaryRequest;
import com.sunday.common_lib.payload.response.AncillaryBulkCreateResponse;
import com.sunday.common_lib.payload.response.AncillaryResponse;
import com.sunday.services.service.AncillaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ancillaries")
@RequiredArgsConstructor
public class AncillaryController {

    private final AncillaryService ancillaryService;

    @PostMapping
    public ResponseEntity<AncillaryResponse> create(
            @Valid @RequestBody AncillaryRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(ancillaryService.create(userId, request));
    }

    @PostMapping("/bulk")
    public ResponseEntity<AncillaryBulkCreateResponse> bulkCreate(
            @Valid @RequestBody List<AncillaryRequest> requests,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(ancillaryService.bulkCreate(userId, requests));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AncillaryResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ancillaryService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<AncillaryResponse>> getAllByAirlineId(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam Long airlineId
    ) {
        return ResponseEntity.ok(ancillaryService.getAllByAirlineId(userId, airlineId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AncillaryResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody AncillaryRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(ancillaryService.update(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        ancillaryService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
