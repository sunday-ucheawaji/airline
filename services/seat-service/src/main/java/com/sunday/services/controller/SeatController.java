package com.sunday.services.controller;

import com.sunday.common_lib.payload.request.SeatRequest;
import com.sunday.common_lib.payload.response.SeatResponse;
import com.sunday.services.service.SeatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    @GetMapping("/{id}")
    public ResponseEntity<SeatResponse> getSeatById(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        return ResponseEntity.ok(seatService.getSeatById(userId, id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SeatResponse> updateSeat(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id,
            @Valid @RequestBody SeatRequest request) {
        return ResponseEntity.ok(seatService.updateSeat(userId, id, request));
    }
}
