package com.sunday.services.controller;

import com.sunday.common_lib.payload.response.CabinSeatStatusResponse;
import com.sunday.common_lib.payload.response.SeatConfirmResponse;
import com.sunday.common_lib.payload.request.CabinAvailabilityRequest;
import com.sunday.common_lib.payload.request.SeatConfirmRequest;
import java.util.UUID;
import com.sunday.common_lib.payload.request.SeatHoldRequest;
import com.sunday.common_lib.payload.response.SeatHoldResponse;
import com.sunday.common_lib.payload.response.SeatInstanceResponse;
import com.sunday.services.service.SeatInstanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Unauthenticated, not routed by the gateway: booking-service (hold, confirm, its own seats) and flight-ops (status summary, search availability). */
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

    /** Free seats in one cabin for each flight instance, for flight search. An instance with no inventory counts 0. */
    @PostMapping("/availability")
    public Map<UUID, Long> countAvailableSeats(@Valid @RequestBody CabinAvailabilityRequest request) {
        return seatInstanceService.countAvailableSeats(request);
    }

    /** Seats held, booked or occupied per flight instance, so flight-ops can refuse changes to departures with sales. */
    @PostMapping("/sold-counts")
    public Map<UUID, Long> countSoldSeats(@RequestBody List<UUID> flightInstanceIds) {
        return seatInstanceService.countSoldSeats(flightInstanceIds);
    }

    @GetMapping("/flight-instance/{flightInstanceId}/status-summary")
    public List<CabinSeatStatusResponse> getSeatStatusSummary(@PathVariable UUID flightInstanceId) {
        return seatInstanceService.getSeatStatusSummary(flightInstanceId);
    }
}
