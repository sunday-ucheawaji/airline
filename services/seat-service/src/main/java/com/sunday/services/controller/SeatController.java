package com.sunday.services.controller;

import com.sunday.common_lib.payload.request.SeatRequest;
import com.sunday.common_lib.payload.response.SeatResponse;
import com.sunday.services.service.SeatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;



    @GetMapping
    public ResponseEntity<List<SeatResponse>> getAllSeats() {
        return ResponseEntity.ok(seatService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SeatResponse> getSeatById(@PathVariable Long id) {
        return ResponseEntity.ok(seatService.getSeatById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SeatResponse> updateSeat(
            @PathVariable Long id,
            @Valid @RequestBody SeatRequest request) {
        return ResponseEntity.ok(seatService.updateSeat(id, request));
    }


}
