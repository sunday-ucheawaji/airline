package com.sunday.services.controller;

import java.util.UUID;
import com.sunday.common_lib.payload.response.PassengerResponse;
import com.sunday.services.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/passengers")
@RequiredArgsConstructor
public class PassengerController {

    private final BookingService bookingService;

    @GetMapping("/booking/{bookingId}")
    public List<PassengerResponse> getPassengersByBooking(
            @PathVariable UUID bookingId,
            @RequestHeader("X-User-Id") UUID userId) {
        return bookingService.getPassengersByBooking(bookingId, userId);
    }
}
