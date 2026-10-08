package com.sunday.services.controller;

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
            @PathVariable Long bookingId,
            @RequestHeader("X-User-Id") Long userId) {
        return bookingService.getPassengersByBooking(bookingId, userId);
    }
}
