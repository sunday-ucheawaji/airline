package com.sunday.services.controller;

import java.util.UUID;
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
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(ancillaryService.create(userId, request));
    }

    @PostMapping("/bulk")
    public ResponseEntity<AncillaryBulkCreateResponse> bulkCreate(
            @Valid @RequestBody List<AncillaryRequest> requests,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(ancillaryService.bulkCreate(userId, requests));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AncillaryResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ancillaryService.getById(id));
    }

    /** Batch variant of {@code GET /{id}}: one round trip instead of one per ancillary. */
    @GetMapping("/all")
    public ResponseEntity<List<AncillaryResponse>> getAllByIds(@RequestParam List<UUID> ids) {
        return ResponseEntity.ok(ancillaryService.getAllByIds(ids));
    }

    @GetMapping
    public ResponseEntity<List<AncillaryResponse>> getAllByAirlineId(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam UUID airlineId
    ) {
        return ResponseEntity.ok(ancillaryService.getAllByAirlineId(userId, airlineId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AncillaryResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody AncillaryRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(ancillaryService.update(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        ancillaryService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
