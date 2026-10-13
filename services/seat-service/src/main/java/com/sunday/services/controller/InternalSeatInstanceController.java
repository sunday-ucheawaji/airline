package com.sunday.services.controller;

import com.sunday.common_lib.payload.response.CabinSeatStatusResponse;
import com.sunday.common_lib.payload.response.SeatConfirmResponse;
import com.sunday.common_lib.payload.request.SeatConfirmRequest;
import java.util.UUID;
import com.sunday.common_lib.payload.request.SeatHoldRequest;
import com.sunday.common_lib.payload.response.SeatHoldResponse;
import com.sunday.common_lib.payload.response.SeatInstanceResponse;
import com.sunday.services.service.SeatInstanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/** Unauthenticated, not routed by the gateway: booking-service (hold, confirm, its own seats) and flight-ops (status summary). */
@RestController
@RequestMapping("/internal/seat-instances")
@RequiredArgsConstructor
public class InternalSeatInstanceController {

    private final SeatInstanceService seatInstanceService;

    @GetMapping("/all")
    public List<SeatInstanceResponse> getAllByIds(@RequestParam List<UUID> Ids) {
        return seatInstanceService.getAllByIdsInternal(Ids);
    }

    @PostMapping("/price/total")
    public BigDecimal calculateSeatPrice(@RequestBody List<UUID> seatInstanceIds) {
        return seatInstanceService.calculateSeatPriceInternal(seatInstanceIds);
    }

    @PostMapping("/hold")
    public SeatHoldResponse holdSeats(@RequestBody SeatHoldRequest request) {
        return seatInstanceService.holdSeats(request);
    }

    @PostMapping("/confirm")
    public SeatConfirmResponse confirmSeats(@RequestBody SeatConfirmRequest request) {
        return seatInstanceService.confirmSeats(request);
    }

    @GetMapping("/flight-instance/{flightInstanceId}/status-summary")
    public List<CabinSeatStatusResponse> getSeatStatusSummary(@PathVariable UUID flightInstanceId) {
        return seatInstanceService.getSeatStatusSummary(flightInstanceId);
    }
}
