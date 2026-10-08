package com.sunday.services.controller;

import com.sunday.common_lib.payload.request.SeatHoldRequest;
import com.sunday.common_lib.payload.response.SeatHoldResponse;
import com.sunday.common_lib.payload.response.SeatInstanceResponse;
import com.sunday.services.service.SeatInstanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/** Unauthenticated, not routed by the gateway. For booking-service pricing and display of a booking's own seats. */
@RestController
@RequestMapping("/internal/seat-instances")
@RequiredArgsConstructor
public class InternalSeatInstanceController {

    private final SeatInstanceService seatInstanceService;

    @GetMapping("/all")
    public List<SeatInstanceResponse> getAllByIds(@RequestParam List<Long> Ids) {
        return seatInstanceService.getAllByIdsInternal(Ids);
    }

    @PostMapping("/price/total")
    public BigDecimal calculateSeatPrice(@RequestBody List<Long> seatInstanceIds) {
        return seatInstanceService.calculateSeatPriceInternal(seatInstanceIds);
    }

    @PostMapping("/hold")
    public SeatHoldResponse holdSeats(@RequestBody SeatHoldRequest request) {
        return seatInstanceService.holdSeats(request);
    }
}
